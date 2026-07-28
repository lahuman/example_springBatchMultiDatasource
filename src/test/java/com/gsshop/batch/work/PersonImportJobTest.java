package com.gsshop.batch.work;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.gsshop.batch.support.BatchIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBatchTest
@SpringBootTest
class PersonImportJobTest extends BatchIntegrationTestSupport {

    @Autowired
    private JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    @Qualifier("businessJdbcTemplate")
    private JdbcTemplate businessJdbcTemplate;

    @Test
    void importsFiveUppercasePeople() throws Exception {
        JobExecution execution = jobOperatorTestUtils.startJob(
                jobOperatorTestUtils.getUniqueJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(businessJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM people", Integer.class)).isEqualTo(5);
        List<String> firstNames = businessJdbcTemplate.queryForList(
                "SELECT first_name FROM people ORDER BY person_id", String.class);
        assertThat(firstNames).containsExactly("JILL", "JOE", "JUSTIN", "JANE", "JOHN");
    }
}
