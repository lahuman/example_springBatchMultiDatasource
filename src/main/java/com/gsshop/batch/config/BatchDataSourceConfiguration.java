package com.gsshop.batch.config;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.batch.autoconfigure.BatchTransactionManager;
import org.springframework.boot.batch.jdbc.autoconfigure.BatchDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;

@Configuration(proxyBeanMethods = false)
public class BatchDataSourceConfiguration {

    @Bean
    @ConfigurationProperties("app.datasource.batch")
    DataSourceProperties batchDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @BatchDataSource
    @DependsOn("databaseDirectory")
    HikariDataSource batchDataSource(
            @Qualifier("batchDataSourceProperties") DataSourceProperties properties) {
        HikariDataSource dataSource = properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        dataSource.setMaximumPoolSize(1);
        dataSource.setPoolName("batch-metadata-pool");
        return dataSource;
    }

    @Bean
    @BatchTransactionManager
    JdbcTransactionManager batchTransactionManager(@BatchDataSource DataSource batchDataSource) {
        return new JdbcTransactionManager(batchDataSource);
    }

    @Bean
    JdbcTemplate batchJdbcTemplate(@BatchDataSource DataSource batchDataSource) {
        return new JdbcTemplate(batchDataSource);
    }
}
