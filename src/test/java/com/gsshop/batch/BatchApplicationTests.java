package com.gsshop.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import javax.sql.DataSource;

import com.gsshop.batch.support.BatchIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootTest
class BatchApplicationTests extends BatchIntegrationTestSupport {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextProvidesExactlyTwoDatabaseBoundaries() {
        Map<String, DataSource> dataSources = applicationContext.getBeansOfType(DataSource.class);
        Map<String, PlatformTransactionManager> transactionManagers =
                applicationContext.getBeansOfType(PlatformTransactionManager.class);

        assertThat(dataSources).containsOnlyKeys("batchDataSource", "businessDataSource");
        assertThat(transactionManagers)
                .containsOnlyKeys("batchTransactionManager", "businessTransactionManager");
    }
}
