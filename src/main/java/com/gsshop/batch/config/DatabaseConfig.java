package com.gsshop.batch.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.batch.BatchDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.transaction.ChainedTransactionManager;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.default")
    DataSource springBatchDb(){
        DataSourceBuilder builder = DataSourceBuilder.create();
        builder.type(HikariDataSource.class);
        return builder.build();
    }

    @Bean
    @BatchDataSource
    @ConfigurationProperties("spring.datasource.work")
    DataSource workDb(){
        DataSourceBuilder builder = DataSourceBuilder.create();
        builder.type(HikariDataSource.class);
        return builder.build();
    }

    // Transaction Setting

    @Bean
    PlatformTransactionManager springBatchTxManager() {
        return new DataSourceTransactionManager(springBatchDb());
    }

    @Bean
    PlatformTransactionManager workTxManager() {
        return new DataSourceTransactionManager(workDb());
    }

    @Bean
    PlatformTransactionManager chainTxManager() {
        ChainedTransactionManager txManager = new ChainedTransactionManager(springBatchTxManager(), workTxManager());
        return txManager;
    }

}
