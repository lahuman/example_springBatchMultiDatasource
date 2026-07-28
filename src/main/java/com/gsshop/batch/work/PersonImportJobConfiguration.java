package com.gsshop.batch.work;

import javax.sql.DataSource;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.builder.SimpleJobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
public class PersonImportJobConfiguration {

    @Bean
    FlatFileItemReader<Person> personReader() {
        return new FlatFileItemReaderBuilder<Person>()
                .name("personReader")
                .resource(new ClassPathResource("sample-data.csv"))
                .delimited(delimited -> delimited.names("firstName", "lastName"))
                .fieldSetMapper(fieldSet -> new Person(
                        fieldSet.readString("firstName"),
                        fieldSet.readString("lastName")))
                .build();
    }

    @Bean
    PersonItemProcessor personProcessor() {
        return new PersonItemProcessor();
    }

    @Bean
    JdbcBatchItemWriter<Person> personWriter(
            @Qualifier("businessDataSource") DataSource businessDataSource) {
        return new JdbcBatchItemWriterBuilder<Person>()
                .dataSource(businessDataSource)
                .sql("""
                        INSERT INTO people (first_name, last_name)
                        VALUES (?, ?)
                        ON CONFLICT(first_name, last_name)
                        DO UPDATE SET last_name = excluded.last_name
                        """)
                .itemPreparedStatementSetter((person, statement) -> {
                    statement.setString(1, person.firstName());
                    statement.setString(2, person.lastName());
                })
                .build();
    }

    @Bean
    Step importPeopleStep(
            JobRepository jobRepository,
            @Qualifier("businessTransactionManager") PlatformTransactionManager transactionManager,
            FlatFileItemReader<Person> personReader,
            PersonItemProcessor personProcessor,
            JdbcBatchItemWriter<Person> personWriter) {
        return new StepBuilder("importPeopleStep", jobRepository)
                .<Person, Person>chunk(10)
                .transactionManager(transactionManager)
                .reader(personReader)
                .processor(personProcessor)
                .writer(personWriter)
                .build();
    }

    @Bean
    Step failureDemoStep(
            JobRepository jobRepository,
            @Qualifier("businessTransactionManager") PlatformTransactionManager transactionManager) {
        return new StepBuilder("failureDemoStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    throw new IllegalStateException(
                            "Intentional failure requested by demo.failure");
                })
                .transactionManager(transactionManager)
                .build();
    }

    @Bean
    Job personImportJob(
            JobRepository jobRepository,
            Step importPeopleStep,
            Step failureDemoStep,
            JobCompletionNotificationListener listener,
            @Value("${demo.failure:false}") boolean failureEnabled) {
        SimpleJobBuilder jobBuilder = new JobBuilder("personImportJob", jobRepository)
                .listener(listener)
                .start(importPeopleStep);
        if (failureEnabled) {
            jobBuilder = jobBuilder.next(failureDemoStep);
        }
        return jobBuilder.build();
    }
}
