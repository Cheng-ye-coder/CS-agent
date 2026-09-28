package com.bitselect.agent.core.parser.mime;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.tika.Tika;

/**
 * 【文件用途】MIME 探测器：按字节 + 文件名探测文档的真实 MIME 类型。
 *
 * 【为什么存在】
 * - 用户上传文件时，Content-Type 是浏览器给的，不可信（可能为空、可能是 application/octet-stream）
 * - 解析路由依赖 MIME 选择解析器，必须拿到"字节层面"的真实类型
 * - 文件扩展名也不可靠（用户可能改了后缀），但可以作为辅助信息提高准确率
 *
 * 【关键设计】
 * - 用 Tika 的字节探测，比扩展名判断准确得多
 * - 同时传入字节和文件名：Tika 会优先用字节内容判断，字节无法判断时回退到文件名扩展名
 * - 产出只服务解析路由，不参与前端展示（展示用的 fileType 是另一回事）
 *
 * 【被谁引用】
 * - IngestionNode（入库时探测真实 MIME）
 * - 上传预处理（前置拦截判断是否支持）
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MimeTypeDetector {

    private static final Tika TIKA = new Tika();

    /**
     * 【方法用途】按字节 + 文件名探测 MIME。
     *
     * @param bytes    文件字节，为空返回 null
     * @param fileName 文件名（可空），用于字节无法判断时按扩展名回退
     * @return MIME 字符串，如 "application/pdf"
     */
    public static String detect(byte[] bytes, String fileName) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        if (fileName == null) {
            return TIKA.detect(bytes);
        }
        return TIKA.detect(bytes, fileName);
    }
}