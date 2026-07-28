package com.gsshop.batch.work;

import static org.assertj.core.api.Assertions.assertThat;

import com.gsshop.batch.support.BatchIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBatchTest
@SpringBootTest(properties = "demo.failure=true")
class FailureDemoJobTest extends BatchIntegrationTestSupport {

    @Autowired
    private JobOperatorTestUtils jobOperatorTestUtils;

    @Test
    void failsOnlyWhenTheDemoOptionIsEnabled() throws Exception {
        JobExecution execution = jobOperatorTestUtils.startJob(
                jobOperatorTestUtils.getUniqueJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(execution.getAllFailureExceptions())
                .extracting(Throwable::getMessage)
                .contains("Intentional failure requested by demo.failure");
    }
}
