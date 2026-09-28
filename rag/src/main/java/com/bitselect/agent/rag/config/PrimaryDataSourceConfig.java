package com.bitselect.agent.rag.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * 显式声明主数据源（bean 名 dataSource），并标记 @Primary。
 *
 * 【为什么需要】
 * 项目里有两个 DataSource：
 *   1. 主数据源：spring.datasource.* 配置（本类负责）
 *   2. bit 数据源：mcp-server 的 BitDataSourceConfig 手动声明（bitDataSource）
 * 容器里已有 DataSource 时，Spring Boot 的 DataSourceAutoConfiguration 会跳过，
 * 导致没有名为 dataSource 的 bean，JdbcTemplate 等按类型注入失败。
 */
@Configuration
public class PrimaryDataSourceConfig {

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName(driverClassName);
        ds.setPoolName("primary-pool");
        ds.setMaximumPoolSize(10);
        ds.setMinimumIdle(5);
        ds.setConnectionTimeout(5000);
        return ds;
    }
}