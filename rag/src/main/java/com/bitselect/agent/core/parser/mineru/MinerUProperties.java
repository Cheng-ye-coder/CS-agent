package com.bitselect.agent.core.parser.mineru;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 【文件用途】MinerU SaaS 配置类：绑定 application.yaml 的 mineru.* 节点。
 *
 * 【为什么存在】
 * - MinerU 的 API 地址、Key、轮询间隔、超时、并发限制都需要可配
 * - 不同环境（开发 / 生产）的并发限制不同
 *
 * 【配置项】
 * - apiUrl / apiKey：SaaS 接入凭证
 * - pollIntervalSeconds / timeoutSeconds：轮询参数
 * - enableTable / enableFormula / ocr / language：解析选项
 * - concurrencyLimit / semaphoreName / maxWaitSeconds / leaseSeconds：并发控制
 *
 * 【关键设计】
 * - ocr 字段命名避坑：Lombok 对 boolean 会自动生成 isOcr() getter，
 *   Spring 按 setter 名 setOcr 识别属性为 "ocr"，故 yaml 必须写 mineru.ocr: false，
 *   不能写 mineru.is-ocr
 *
 * 【被谁引用】MinerUClient、MinerUDocumentParser、MinerUPollingExecutor。
 */
@Configuration
@ConfigurationProperties("mineru")
@Data
public class MinerUProperties {

    /**
     * MinerU SaaS API 根地址，默认 https://mineru.net/api/v4
     */
    private String apiUrl = "https://mineru.net/api/v4";

    /**
     * API token，从环境变量 MINERU_API_KEY 注入
     */
    private String apiKey;

    /**
     * 内部轮询间隔（秒），默认 5
     */
    private int pollIntervalSeconds = 5;

    /**
     * 单任务超时（秒），默认 300
     */
    private int timeoutSeconds = 300;

    /**
     * 是否提取表格，默认 true
     */
    private boolean enableTable = true;

    /**
     * 是否提取公式，默认 true
     */
    private boolean enableFormula = true;

    /**
     * 是否强制 OCR，默认 false（原生文本 PDF 不需要）
     *
     * 字段名不带 is 前缀：Lombok 对 boolean 字段自动加 is 前缀生成 getter isOcr()，
     * setter 为 setOcr(boolean)；Spring @ConfigurationProperties 据 setter 名识别属性为 ocr，
     * 故 yaml 必须写 mineru.ocr: false 而非 is-ocr
     */
    private boolean ocr = false;

    /**
     * 语言代码，遵循 MinerU（PaddleOCR）规范，默认 ch（中英文）
     */
    private String language = "ch";

    /**
     * 全局 outstanding 任务上限，防止打爆 SaaS，默认 16
     */
    private int concurrencyLimit = 16;

    /**
     * MinerU 解析分布式信号量名称
     */
    private String semaphoreName = "rag:mineru:parse";

    /**
     * 获取 MinerU 解析许可最大等待时间（秒）
     */
    private int maxWaitSeconds = 30;

    /**
     * MinerU 解析许可自动释放时间（秒），需大于 timeoutSeconds
     */
    private int leaseSeconds = 900;
}