package com.bitselect.agent.core.parser.model;

/**
 * 【文件用途】资源引用：指向对象存储中已上传的二进制资源（图片等）。
 *
 * 【为什么存在】
 * - 图片 Block 需要携带"图片的访问地址 + 类型"，直接用 String 表达不了两个字段
 * - 未来可能加缩略图 URL、尺寸、上传时间等，封装成对象后扩展不破调用方
 *
 * 【被谁引用】
 * - ImageBlock（字段 asset）
 * - ChunkMetadata（字段 assets，落进向量库 metadata）
 *
 * 【构造时机】解析器上传图片到对象存储后构造，随 Block 进入后续阶段。
 *
 * @param publicUrl 浏览器可直连的公开预览 URL，形如 http://localhost:9000/ragent-assets/xxx.png
 * @param mime      资源 MIME 类型，前端据此决定用 <img> 还是 <video> 渲染
 */
public record AssetRef(String publicUrl, String mime) {
}