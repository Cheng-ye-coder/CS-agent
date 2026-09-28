package com.bitselect.agent.mcp.config.bit;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.spring.MybatisSqlSessionFactoryBean;
import com.bitselect.agent.mcp.dao.handler.BitMetaObjectHandler;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

/**
 * 比特严选业务库的数据源
 * <p>
 * 启动时先初始化业务库表结构，再创建连接池
 */
@Configuration
@EnableConfigurationProperties(BitProperties.class)
@MapperScan(
    basePackages = "com.bitselect.agent.mcp.dao.mapper",
    sqlSessionFactoryRef = "bitSqlSessionFactory"
)
public class BitDataSourceConfig {

    @Bean
    public DataSource bitDataSource(BitProperties properties) {
        new BitSchemaInitializer(properties.getDatasource()).initialize();
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setPoolName("bit-pool");
        dataSource.setJdbcUrl(properties.getDatasource().getUrl());
        dataSource.setUsername(properties.getDatasource().getUsername());
        dataSource.setPassword(properties.getDatasource().getPassword());
        dataSource.setMaximumPoolSize(8);
        return dataSource;
    }

    @Bean
    public SqlSessionFactory bitSqlSessionFactory(@Qualifier("bitDataSource") DataSource bitDataSource) throws Exception {
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setMetaObjectHandler(new BitMetaObjectHandler());

        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(bitDataSource);
        factory.setConfiguration(new MybatisConfiguration());
        factory.setGlobalConfig(globalConfig);
        return factory.getObject();
    }

    @Bean
    public SqlSessionTemplate bitSqlSessionTemplate(@Qualifier("bitSqlSessionFactory") SqlSessionFactory bitSqlSessionFactory) {
        return new SqlSessionTemplate(bitSqlSessionFactory);
    }

    @Bean
    public PlatformTransactionManager bitTransactionManager(@Qualifier("bitDataSource") DataSource bitDataSource) {
        return new DataSourceTransactionManager(bitDataSource);
    }

    @Bean
    public TransactionTemplate bitTransactionTemplate(@Qualifier("bitTransactionManager") PlatformTransactionManager bitTransactionManager) {
        return new TransactionTemplate(bitTransactionManager);
    }
}