package com.gsshop.batch.config;

import java.util.List;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.sql.init.DatabaseInitializationMode;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;

@Configuration(proxyBeanMethods = false)
public class BusinessDataSourceConfiguration {

    @Bean
    @ConfigurationProperties("app.datasource.business")
    DataSourceProperties businessDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    @DependsOn("databaseDirectory")
    HikariDataSource businessDataSource(
            @Qualifier("businessDataSourceProperties") DataSourceProperties properties) {
        HikariDataSource dataSource = properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        dataSource.setMaximumPoolSize(1);
        dataSource.setPoolName("business-data-pool");
        return dataSource;
    }

    @Bean
    @Primary
    JdbcTransactionManager businessTransactionManager(
            @Qualifier("businessDataSource") DataSource businessDataSource) {
        return new JdbcTransactionManager(businessDataSource);
    }

    @Bean
    JdbcTemplate businessJdbcTemplate(
            @Qualifier("businessDataSource") DataSource businessDataSource) {
        return new JdbcTemplate(businessDataSource);
    }

    @Bean
    DataSourceScriptDatabaseInitializer businessDataSourceInitializer(
            @Qualifier("businessDataSource") DataSource businessDataSource) {
        DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
        settings.setMode(DatabaseInitializationMode.ALWAYS);
        settings.setSchemaLocations(List.of("classpath:business-schema-sqlite.sql"));
        return new DataSourceScriptDatabaseInitializer(businessDataSource, settings);
    }
}
