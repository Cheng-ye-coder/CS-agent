package com.bitselect.agent.rag.config;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * 显式声明主库（cs_agent）的 SqlSessionFactory，bean 名固定为 sqlSessionFactory。
 *
 * 【为什么需要】
 * BitDataSourceConfig 手动定义了 bitSqlSessionFactory，
 * 导致 MybatisPlusAutoConfiguration 检测到已有 SqlSessionFactory 而跳过自动配置，
 * 结果所有未指定 sqlSessionFactoryRef 的 Mapper 全都退化到用 bit 数据源。
 */
@Configuration
public class PrimaryMybatisConfig {

    @Bean("sqlSessionFactory")
    @Primary
    public SqlSessionFactory sqlSessionFactory(
            @Qualifier("dataSource") DataSource dataSource,
            ObjectProvider<MybatisPlusInterceptor> interceptorProvider,
            ObjectProvider<MetaObjectHandler> metaObjectHandlerProvider) throws Exception {

        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);

        GlobalConfig globalConfig = new GlobalConfig();
        GlobalConfig.DbConfig dbConfig = new GlobalConfig.DbConfig();
        dbConfig.setLogicDeleteField("deleted");
        dbConfig.setLogicDeleteValue("1");
        dbConfig.setLogicNotDeleteValue("0");
        globalConfig.setDbConfig(dbConfig);
        metaObjectHandlerProvider.ifAvailable(globalConfig::setMetaObjectHandler);

        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfiguration(configuration);
        factory.setGlobalConfig(globalConfig);
        interceptorProvider.ifAvailable(factory::setPlugins);

        return factory.getObject();
    }
}