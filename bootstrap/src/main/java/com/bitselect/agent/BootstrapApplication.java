package com.bitselect.agent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * CS-agent 核心应用启动类
 *
 * <p>设计说明：</p>
 * <ul>
 *   <li>@MapperScan 精确列出各业务模块的 dao.mapper 包</li>
 *   <li>不扫 com.bitselect.agent.mcp.dao.mapper —— 它由 BitDataSourceConfig 单独管理，走 bit 数据源</li>
 *   <li>配置文件分散在各模块的 src/main/resources 下，bootstrap 只做启动</li>
 * </ul>
 */
@SpringBootApplication(scanBasePackages = "com.bitselect.agent")
@MapperScan(
    basePackages = {
        "com.bitselect.agent.dao.mapper",
        "com.bitselect.agent.ingestion.dao.mapper",
        "com.bitselect.agent.knowledge.dao.mapper",
        "com.bitselect.agent.rag.dao.mapper",
        "com.bitselect.agent.audit.dao.mapper",
        "com.bitselect.agent.sample.dao.mapper",
        "com.bitselect.agent.user.dao.mapper"
    },
    sqlSessionFactoryRef = "sqlSessionFactory"
)
@EnableScheduling
public class BootstrapApplication {

    public static void main(String[] args) {
        SpringApplication.run(BootstrapApplication.class, args);
    }
}