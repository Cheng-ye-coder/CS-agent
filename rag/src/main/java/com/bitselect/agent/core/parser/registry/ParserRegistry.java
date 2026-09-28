package com.bitselect.agent.core.parser.registry;

import com.bitselect.agent.core.parser.DocumentParser;
import com.bitselect.agent.framework.exception.ClientException;
import com.bitselect.agent.framework.exception.ServiceException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.mime.MimeTypes;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * 【文件用途】解析器注册表：(MIME × 解析档位) → 解析器。
 *
 * 【为什么存在】
 * - 上传文档时，需要根据 MIME 和用户选的档位，找到对应解析器
 * - 需要在启动期就发现"认领冲突"（两个解析器都认领同一个键）
 * - 需要兜底策略：请求档位没找到时，自动回落到 FAST 档
 *
 * 【关键设计】
 * - 启动期建表：遍历所有 DocumentParser 实现，按 supportedMimeTypes() 建两张表
 *   - exact：档位 → (完整 MIME → 解析器)
 *   - wildcard：档位 → (MIME 大类前缀 → 解析器)，如 "text" → Tika
 * - 键冲突即启动失败（fail-fast），不等运行时才发现
 * - 查找顺序四档：精确+请求档位 → 通配+请求档位 → 精确+FAST → 通配+FAST → 抛错
 * - 启动自检：对外声明的扩展名清单，每个都必须被某个解析器精确认领
 *   清单落在扩展名而非 MIME 上，因为 MIME 是探测器的产出，拿它校验它自己恒为真
 *
 * 【被谁引用】
 * - IngestionNode（入库时按 MIME 选解析器）
 * - IngestionSpecSchemaProvider（查询哪些 MIME 支持非兜底档）
 */
@Slf4j
@Component
public class ParserRegistry {

    private static final String WILDCARD_SUFFIX = "/*";

    /**
     * 【自检清单】对外声明支持的扩展名，每个都必须被某个解析器精确认领，缺一个启动即失败。
     *
     * 落在扩展名而非 MIME 上，因为 MIME 是探测器的产出，拿它校验它自己恒为真。
     */
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "ppt", "pptx",
            "xls", "xlsx", "csv",
            "md", "markdown", "txt", "text",
            "html", "htm", "json", "xml", "rtf",
            "png", "jpg", "jpeg", "svg"
    );

    /**
     * 精确表：档位 → (全量 MIME → 解析器)
     */
    private final Map<ParseProfile, Map<String, DocumentParser>> exact = new EnumMap<>(ParseProfile.class);

    /**
     * 通配表：档位 → (MIME 大类前缀 → 解析器)，如 "text" → Tika
     */
    private final Map<ParseProfile, Map<String, DocumentParser>> wildcard = new EnumMap<>(ParseProfile.class);

    /**
     * 【构造逻辑】启动时遍历所有解析器，按 supportedMimeTypes() 建表。
     */
    public ParserRegistry(List<DocumentParser> parsers) {
        for (DocumentParser parser : parsers) {
            Map<ParseProfile, Set<String>> claims = parser.supportedMimeTypes();
            if (claims == null || claims.isEmpty()) {
                throw new ServiceException("解析器未声明任何 (MIME × 档位) 认领：" + parser.getParserType());
            }
            claims.forEach((profile, mimeTypes) -> register(parser, profile, mimeTypes));
        }
    }

    /**
     * 【方法用途】注册单个解析器：写进 exact 表或 wildcard 表，同键冲突即报错。
     */
    private void register(DocumentParser parser, ParseProfile profile, Set<String> mimeTypes) {
        if (profile == null || mimeTypes == null || mimeTypes.isEmpty()) {
            throw new ServiceException("解析器认领清单存在空档位或空 MIME 集合：" + parser.getParserType());
        }
        for (String raw : mimeTypes) {
            if (!StringUtils.hasText(raw)) {
                throw new ServiceException("解析器认领了空 MIME：" + parser.getParserType());
            }
            String mime = raw.trim().toLowerCase(Locale.ROOT);
            boolean isWildcard = mime.endsWith(WILDCARD_SUFFIX);
            String key = isWildcard ? mime.substring(0, mime.length() - WILDCARD_SUFFIX.length()) : mime;
            Map<String, DocumentParser> table = (isWildcard ? wildcard : exact)
                    .computeIfAbsent(profile, k -> new HashMap<>());
            DocumentParser previous = table.put(key, parser);
            if (previous != null && previous != parser) {
                throw new ServiceException(String.format(
                        "解析器路由键冲突：档位=%s MIME=%s 同时被 %s 与 %s 认领，请显式区分",
                        profile.getCode(), raw, previous.getParserType(), parser.getParserType()));
            }
        }
    }

    /**
     * 【方法用途】启动自检：确认所有声明支持的扩展名，都能被某个解析器精确认领。
     *
     * 自检失败直接阻止启动，避免运行期用户上传文档后才发现"无人解析"。
     */
    @PostConstruct
    public void selfCheck() {
        MimeTypes mimeTypes = MimeTypes.getDefaultMimeTypes();
        Tika tika = new Tika(mimeTypes);
        Map<String, DocumentParser> exactFallback = exact.getOrDefault(ParseProfile.FAST, Map.of());
        List<String> unclaimed = new ArrayList<>();
        for (String extension : new TreeSet<>(SUPPORTED_EXTENSIONS)) {
            String mime = tika.detect("probe." + extension);
            if (!exactFallback.containsKey(mime)) {
                unclaimed.add("." + extension + " → " + mime);
            }
        }
        if (!unclaimed.isEmpty()) {
            throw new ServiceException("解析器注册表自检失败，以下扩展名探测出的 MIME 无人精确认领，"
                    + "只会落到通配兜底：" + String.join(", ", unclaimed));
        }
        // 非兜底档只认精确 MIME：通配认领没法枚举成具体格式，档位差异会对外不可见
        List<String> illegalWildcards = new ArrayList<>();
        wildcard.forEach((profile, table) -> {
            if (profile != ParseProfile.FAST) {
                table.keySet().forEach(prefix -> illegalWildcards.add(profile.getCode() + " → " + prefix + WILDCARD_SUFFIX));
            }
        });
        if (!illegalWildcards.isEmpty()) {
            throw new ServiceException("非兜底档只允许精确 MIME 认领，以下通配认领无法枚举成具体格式："
                    + String.join(", ", illegalWildcards));
        }
        log.info("解析器注册表就绪 精确键={} 通配键={} 自检扩展名={} 全部精确命中",
                countKeys(exact), countKeys(wildcard), SUPPORTED_EXTENSIONS.size());
    }

    /**
     * 【方法用途】按 (MIME × 档位) 查找解析器，找不到回落到 FAST 档，再找不到返回 Optional.empty()。
     *
     * @param mimeType 真实 MIME，允许带 ;charset= 参数
     * @param profile  请求档位，为空按默认档
     */
    public Optional<DocumentParser> find(String mimeType, ParseProfile profile) {
        String mime = normalize(mimeType);
        if (mime == null) {
            return Optional.empty();
        }
        ParseProfile requested = profile == null ? ParseProfile.defaultProfile() : profile;
        DocumentParser hit = lookup(mime, requested);
        if (hit == null && requested != ParseProfile.FAST) {
            hit = lookup(mime, ParseProfile.FAST);
        }
        return Optional.ofNullable(hit);
    }

    /**
     * 【方法用途】按 (MIME × 档位) 查找解析器，认不出来就报错。
     *
     * 不塞给某个通用解析器产出垃圾文本——宁可显式失败让用户看到清晰的报错。
     */
    public DocumentParser require(String mimeType, ParseProfile profile) {
        return find(mimeType, profile).orElseThrow(() -> new ClientException(
                "未找到 MIME [" + mimeType + "] 在档位 ["
                        + (profile == null ? ParseProfile.defaultProfile() : profile).getCode() + "] 下对应的解析器"));
    }

    /**
     * 【方法用途】找出"档位真正有区别的 MIME"：某个非兜底档命中的解析器 ≠ 兜底档命中的解析器。
     *
     * 供上层决定"解析档位"这个选项该不该给用户看：两档命中同一解析器时档位是空操作。
     */
    public Set<String> profileSensitiveMimeTypes() {
        Set<String> sensitive = new TreeSet<>();
        for (ParseProfile profile : ParseProfile.values()) {
            if (profile == ParseProfile.FAST) {
                continue;
            }
            exact.getOrDefault(profile, Map.of()).forEach((mime, parser) -> {
                if (lookup(mime, ParseProfile.FAST) != parser) {
                    sensitive.add(mime);
                }
            });
        }
        return sensitive;
    }

    /**
     * 【方法用途】该 MIME 是否有任何档位可解析，供上传前置拦截使用。
     */
    public boolean canParse(String mimeType) {
        String mime = normalize(mimeType);
        if (mime == null) {
            return false;
        }
        for (ParseProfile profile : ParseProfile.values()) {
            if (lookup(mime, profile) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * 【方法用途】单档位查找：精确键优先于通配键。
     */
    private DocumentParser lookup(String mime, ParseProfile profile) {
        DocumentParser hit = exact.getOrDefault(profile, Map.of()).get(mime);
        if (hit != null) {
            return hit;
        }
        int slash = mime.indexOf('/');
        if (slash <= 0) {
            return null;
        }
        return wildcard.getOrDefault(profile, Map.of()).get(mime.substring(0, slash));
    }

    /**
     * 【方法用途】MIME 归一化：小写、去 charset 参数。
     */
    private static String normalize(String mimeType) {
        if (!StringUtils.hasText(mimeType)) {
            return null;
        }
        String normalized = mimeType.trim().toLowerCase(Locale.ROOT);
        int semicolon = normalized.indexOf(';');
        if (semicolon >= 0) {
            normalized = normalized.substring(0, semicolon).trim();
        }
        return normalized.isEmpty() ? null : normalized;
    }

    private static int countKeys(Map<ParseProfile, Map<String, DocumentParser>> table) {
        return table.values().stream().mapToInt(Map::size).sum();
    }
}