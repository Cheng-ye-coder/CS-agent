package com.bitselect.agent.rag.core.prompt;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 【文件用途】提示模板加载器：从 classpath 加载 .st 模板文件，支持变量填充与 section 渲染。
 *
 * 【为什么存在】
 * - 所有 Prompt 模板都放在 resources/prompt/*.st
 * - 加载后缓存到内存，避免重复 IO
 * - 支持 section 解析（context-format.st 用多 section 结构）
 *
 * 【被谁引用】DefaultContextFormatter / RAGPromptService / DefaultIntentClassifier / ConversationTitleGenerator
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptTemplateLoader {

    private final ResourceLoader resourceLoader;
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> sectionCache = new ConcurrentHashMap<>();

    public String load(String path) {
        if (StrUtil.isBlank(path)) {
            throw new IllegalArgumentException("提示模板路径为空");
        }
        return cache.computeIfAbsent(path, this::readResource);
    }

    public String render(String path, Map<String, String> slots) {
        String template = load(path);
        String filled = PromptTemplateUtils.fillSlots(template, slots);
        return PromptTemplateUtils.cleanupPrompt(filled);
    }

    public String loadSection(String path, String section) {
        Map<String, String> sections = sectionCache.computeIfAbsent(path, p -> {
            String content = load(p);
            return PromptTemplateUtils.parseSections(content);
        });
        String template = sections.get(section);
        if (template == null) {
            throw new IllegalStateException("模板 section 不存在：" + path + " -> " + section);
        }
        return template;
    }

    public String renderSection(String path, String section, Map<String, String> slots) {
        String template = loadSection(path, section);
        String filled = PromptTemplateUtils.fillSlots(template, slots);
        return PromptTemplateUtils.cleanupPrompt(filled);
    }

    private String readResource(String path) {
        String location = path.startsWith("classpath:") ? path : "classpath:" + path;
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new IllegalStateException("提示词模板路径不存在：" + path);
        }
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("读取提示模板失败，路径：{}", path, e);
            throw new IllegalStateException("读取提示模板失败，路径：" + path, e);
        }
    }
}