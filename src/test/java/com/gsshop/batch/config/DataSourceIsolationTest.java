package com.gsshop.batch.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.sql.DataSource;

import com.gsshop.batch.support.BatchIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.batch.jdbc.autoconfigure.BatchDataSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class DataSourceIsolationTest extends BatchIntegrationTestSupport {

    @Test
    void keepsBatchAndBusinessSchemasInDifferentDatabases(
            @BatchDataSource DataSource batchDataSource,
            @Qualifier("businessDataSource") DataSource businessDataSource,
            @Qualifier("batchJdbcTemplate") JdbcTemplate batchJdbcTemplate,
            @Qualifier("businessJdbcTemplate") JdbcTemplate businessJdbcTemplate) throws SQLException {

        assertThat(tableNames(batchDataSource)).contains("BATCH_JOB_INSTANCE");
        assertThat(tableNames(batchDataSource)).doesNotContain("PEOPLE");
        assertThat(tableNames(businessDataSource)).contains("PEOPLE");
        assertThat(tableNames(businessDataSource)).doesNotContain("BATCH_JOB_INSTANCE");
        assertThat(batchJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM BATCH_JOB_EXECUTION", Integer.class)).isNotNegative();
        assertThat(businessJdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM sqlite_master
                WHERE type = 'table' AND UPPER(name) LIKE 'BATCH_%'
                """, Integer.class)).isZero();
    }

    private Set<String> tableNames(DataSource dataSource) throws SQLException {
        Set<String> names = new HashSet<>();
        try (Connection connection = dataSource.getConnection();
                ResultSet tables = connection.getMetaData().getTables(null, null, "%", new String[] {"TABLE"})) {
            while (tables.next()) {
                names.add(tables.getString("TABLE_NAME").toUpperCase(Locale.ROOT));
            }
        }
        return names;
    }
}
