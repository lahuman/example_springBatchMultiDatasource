# Spring Batch Multi-DataSource Modernization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the five-year-old sample into a secure, tested Spring Boot 4.1 / Spring Batch 6 reference that uses file-backed H2 metadata and SQLite business data without an installed database server.

**Architecture:** Spring Boot auto-configures the JDBC `JobRepository` from an H2 `@BatchDataSource` and `@BatchTransactionManager`. A separate primary SQLite data source and transaction manager own the chunk writes; idempotent upserts make repeated Job instances safe without pretending the two databases share one atomic transaction.

**Tech Stack:** Java 21, Spring Boot 4.1.0, Spring Batch 6.0.4, Gradle 9.6.1, H2 managed by the Boot BOM, Xerial SQLite JDBC 3.53.2.0, JUnit Jupiter, GitHub Actions, Dependabot

## Global Constraints

- Local runs persist H2 and SQLite files only below `./data/`.
- Tests use fresh files below a JUnit-created temporary directory and never use `./data/`.
- H2 contains Spring Batch metadata only; SQLite contains the `people` business table only.
- `demo.failure=false` is the default; `demo.failure=true` adds an intentional failing step.
- Repeated imports keep exactly five unique people while H2 retains each execution.
- Do not use XA or `ChainedTransactionManager`; document the two local transaction boundaries.
- Use the Spring Boot BOM for every managed dependency and pin only SQLite JDBC 3.53.2.0 explicitly.
- Remove all remote database endpoints and credentials from the working tree.
- README body is Korean with an English summary and quick start at the top.
- Do not add a software license without an explicit owner decision.

---

## File Structure

### Build and repository automation

- `build.gradle`: Java toolchain, Boot plugin, minimal dependencies, dependency locking.
- `gradle/wrapper/gradle-wrapper.properties`: Gradle 9.6.1 URL and official distribution checksum.
- `gradle/wrapper/gradle-wrapper.jar`, `gradlew`, `gradlew.bat`: regenerated Gradle 9.6.1 wrapper files.
- `gradle.lockfile`: resolved dependency lock state.
- `.gitignore`: excludes local database files and build output.
- `.github/workflows/build.yml`: JDK 21 build, test, and wrapper validation.
- `.github/workflows/dependency-submission.yml`: submits the Gradle graph to GitHub.
- `.github/dependabot.yml`: weekly Gradle and GitHub Actions updates.

### Runtime code and resources

- `src/main/java/com/gsshop/batch/config/BatchDataSourceConfiguration.java`: H2 Batch data source and transaction manager.
- `src/main/java/com/gsshop/batch/config/BusinessDataSourceConfiguration.java`: SQLite data source, transaction manager, JDBC operations, and schema initializer.
- `src/main/java/com/gsshop/batch/config/DatabaseDirectoryConfiguration.java`: creates the configured local database directory before either data source starts.
- `src/main/java/com/gsshop/batch/work/Person.java`: immutable Java record.
- `src/main/java/com/gsshop/batch/work/PersonItemProcessor.java`: validation and locale-stable uppercase conversion.
- `src/main/java/com/gsshop/batch/work/PersonImportJobConfiguration.java`: reader, writer, Steps, and Job flow.
- `src/main/java/com/gsshop/batch/work/JobCompletionNotificationListener.java`: terminal status and business row-count logging.
- `src/main/resources/application.yml`: safe local file URLs and Batch settings.
- `src/main/resources/business-schema-sqlite.sql`: idempotent SQLite schema.
- `src/main/resources/sample-data.csv`: unchanged five-person input, normalized with a final newline.

### Tests and documentation

- `src/test/java/com/gsshop/batch/support/BatchIntegrationTestSupport.java`: per-test-class temporary JDBC URLs.
- `src/test/java/com/gsshop/batch/config/DataSourceIsolationTest.java`: bean and schema separation.
- `src/test/java/com/gsshop/batch/work/PersonItemProcessorTest.java`: processor unit tests.
- `src/test/java/com/gsshop/batch/work/PersonImportJobTest.java`: success and repeated-run integration tests.
- `src/test/java/com/gsshop/batch/work/FailureDemoJobTest.java`: intentional failure integration test.
- `src/test/java/com/gsshop/batch/BatchApplicationTests.java`: minimal default-context smoke test.
- `src/test/resources/application.yml`: disables automatic Job startup in tests.
- `README.md`: verified learning path and migration guide.
- `CONTRIBUTING.md`: contributor build and review contract.

### Removed obsolete files

- `src/main/java/com/gsshop/batch/config/DatabaseConfig.java`
- `src/main/java/com/gsshop/batch/work/BatchConfiguration.java`
- `src/main/resources/schema-all.sql`

---

### Task 1: Modernize and secure the build

**Files:**

- Modify: `build.gradle`
- Modify: `gradle/wrapper/gradle-wrapper.properties`
- Regenerate: `gradle/wrapper/gradle-wrapper.jar`
- Regenerate: `gradlew`
- Regenerate: `gradlew.bat`
- Modify: `.gitignore`
- Create after dependency resolution: `gradle.lockfile`

**Interfaces:**

- Produces: a Java 21 Gradle 9.6.1 build with Boot-managed Spring Batch 6.0.4 dependencies.
- Consumes: no application interfaces.

- [ ] **Step 1: Record the expected baseline failure**

Run:

```bash
GRADLE_USER_HOME=/tmp/spring-batch-modernization-gradle ./gradlew test
```

Expected before the change: failure containing `Unsupported class file major version 65` because Gradle 6.8.2 cannot run on JDK 21.

- [ ] **Step 2: Replace the build with the minimal Boot 4.1 dependency set**

Use this `build.gradle`:

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.1.0'
}

group = 'com.gsshop'
version = '1.0.0-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-batch-jdbc'
    implementation 'org.springframework.boot:spring-boot-starter-jdbc'

    runtimeOnly 'com.h2database:h2'
    runtimeOnly 'org.xerial:sqlite-jdbc:3.53.2.0'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-batch-test'
}

dependencyLocking {
    lockAllConfigurations()
}

tasks.named('test') {
    useJUnitPlatform()
}
```

Do not retain JPA, Lombok, Oracle, MySQL, or a separately versioned Spring dependency-management plugin.

- [ ] **Step 3: Bootstrap the Gradle 9.6.1 wrapper**

First change `gradle/wrapper/gradle-wrapper.properties` so the old wrapper downloads a JDK-21-compatible distribution:

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-9.6.1-bin.zip
distributionSha256Sum=9c0f7faeeb306cb14e4279a3e084ca6b596894089a0638e68a07c945a32c9e14
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

Then regenerate every wrapper artifact:

```bash
GRADLE_USER_HOME=/tmp/spring-batch-modernization-gradle ./gradlew wrapper --gradle-version 9.6.1 --distribution-type bin --gradle-distribution-sha256-sum 9c0f7faeeb306cb14e4279a3e084ca6b596894089a0638e68a07c945a32c9e14
GRADLE_USER_HOME=/tmp/spring-batch-modernization-gradle ./gradlew wrapper
```

Verify:

```bash
shasum -a 256 gradle/wrapper/gradle-wrapper.jar
```

Expected wrapper JAR checksum:

```text
497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7
```

- [ ] **Step 4: Ignore only generated local state**

Ensure `.gitignore` contains:

```gitignore
.gradle/
build/
data/
*.iml
.idea/
```

Do not ignore `gradle.lockfile`, wrapper artifacts, source files, test reports referenced by CI, or documentation.

- [ ] **Step 5: Verify dependency resolution and write the lock state**

Run:

```bash
GRADLE_USER_HOME=/tmp/spring-batch-modernization-gradle ./gradlew dependencies --write-locks
GRADLE_USER_HOME=/tmp/spring-batch-modernization-gradle ./gradlew dependencyInsight --dependency spring-batch-core --configuration runtimeClasspath
```

Expected: `spring-batch-core:6.0.4`, a created `gradle.lockfile`, and no Oracle, MySQL, JPA, or Lombok artifact in the direct dependency declarations.

- [ ] **Step 6: Commit the build modernization**

```bash
git add build.gradle gradle/wrapper/gradle-wrapper.properties gradle/wrapper/gradle-wrapper.jar gradlew gradlew.bat gradle.lockfile .gitignore
git commit -m "build: upgrade to Spring Boot 4.1 and Java 21"
```

---

### Task 2: Establish isolated H2 and SQLite infrastructure

**Files:**

- Delete: `src/main/java/com/gsshop/batch/config/DatabaseConfig.java`
- Create: `src/main/java/com/gsshop/batch/config/BatchDataSourceConfiguration.java`
- Create: `src/main/java/com/gsshop/batch/config/BusinessDataSourceConfiguration.java`
- Create: `src/main/java/com/gsshop/batch/config/DatabaseDirectoryConfiguration.java`
- Delete: `src/main/resources/schema-all.sql`
- Create: `src/main/resources/business-schema-sqlite.sql`
- Replace: `src/main/resources/application.yml`
- Create: `src/test/resources/application.yml`
- Create: `src/test/java/com/gsshop/batch/support/BatchIntegrationTestSupport.java`
- Create: `src/test/java/com/gsshop/batch/config/DataSourceIsolationTest.java`

**Interfaces:**

- Produces: beans named `databaseDirectory`, `batchDataSource`, `batchTransactionManager`, `batchJdbcTemplate`, `businessDataSource`, `businessTransactionManager`, and `businessJdbcTemplate`.
- Produces: SQLite table `people(person_id, first_name, last_name)` with `UNIQUE(first_name, last_name)`.
- Consumes: Spring Boot `@BatchDataSource` and `@BatchTransactionManager` qualifiers.

- [ ] **Step 1: Write the failing data-source isolation test**

Create a test support base that supplies unique file URLs:

```java
package com.gsshop.batch.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public abstract class BatchIntegrationTestSupport {

    private static final Path DATABASE_DIRECTORY = createDatabaseDirectory();

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("app.data-directory", DATABASE_DIRECTORY::toString);
        registry.add("app.datasource.batch.url",
                () -> "jdbc:h2:file:" + DATABASE_DIRECTORY.resolve("batch-meta"));
        registry.add("app.datasource.business.url",
                () -> "jdbc:sqlite:" + DATABASE_DIRECTORY.resolve("business.db"));
    }

    private static Path createDatabaseDirectory() {
        try {
            return Files.createTempDirectory("spring-batch-multidatasource-");
        }
        catch (IOException exception) {
            throw new IllegalStateException("Could not create test database directory", exception);
        }
    }
}
```

In `DataSourceIsolationTest`, start the full context and assert:

```java
@SpringBootTest
class DataSourceIsolationTest extends BatchIntegrationTestSupport {

    @Test
    void keepsBatchAndBusinessSchemasInDifferentDatabases(
            @BatchDataSource DataSource batchDataSource,
            @Qualifier("businessDataSource") DataSource businessDataSource) throws SQLException {

        assertThat(tableNames(batchDataSource)).contains("BATCH_JOB_INSTANCE");
        assertThat(tableNames(batchDataSource)).doesNotContain("PEOPLE");
        assertThat(tableNames(businessDataSource)).contains("PEOPLE");
        assertThat(tableNames(businessDataSource)).doesNotContain("BATCH_JOB_INSTANCE");
    }
}
```

Implement `tableNames(DataSource)` in the test using `Connection.getMetaData().getTables(...)` and uppercase every returned table name with `Locale.ROOT`.

- [ ] **Step 2: Run the isolation test to verify it fails**

```bash
GRADLE_USER_HOME=/tmp/spring-batch-modernization-gradle ./gradlew test --tests '*DataSourceIsolationTest'
```

Expected: compilation or context failure because the new qualified beans and schemas do not exist.

- [ ] **Step 3: Add safe file-backed configuration**

Replace `application.yml` with:

```yaml
spring:
  application:
    name: spring-batch-multi-datasource
  main:
    web-application-type: none
  batch:
    job:
      name: personImportJob
    jdbc:
      initialize-schema: always

app:
  data-directory: ./data
  datasource:
    batch:
      url: jdbc:h2:file:./data/batch-meta;AUTO_SERVER=TRUE
      username: sa
      password: ""
      driver-class-name: org.h2.Driver
    business:
      url: jdbc:sqlite:./data/business.db
      driver-class-name: org.sqlite.JDBC

demo:
  failure: false
```

Create `src/test/resources/application.yml`:

```yaml
spring:
  batch:
    job:
      enabled: false
    jdbc:
      initialize-schema: always

demo:
  failure: false
```

No environment-specific remote URL, username, or password may remain.

- [ ] **Step 4: Create the configured database directory**

`DatabaseDirectoryConfiguration` exposes a bean named `databaseDirectory`:

```java
@Bean
Path databaseDirectory(@Value("${app.data-directory:./data}") Path directory) {
    try {
        return Files.createDirectories(directory.toAbsolutePath().normalize());
    }
    catch (IOException exception) {
        throw new IllegalStateException(
                "Could not create database directory: " + directory, exception);
    }
}
```

Mark both data source beans with `@DependsOn("databaseDirectory")`. This makes
directory creation part of application startup and makes an unwritable path
fail before a Job is launched. The test support overrides
`app.data-directory`, so tests do not create the repository's `./data/`.

- [ ] **Step 5: Configure the H2 Batch infrastructure**

`BatchDataSourceConfiguration` binds `app.datasource.batch` to a
`DataSourceProperties` bean and builds a single-connection Hikari data source.
The essential bean signatures are:

```java
@Bean
@ConfigurationProperties("app.datasource.batch")
DataSourceProperties batchDataSourceProperties()

@Bean
@BatchDataSource
HikariDataSource batchDataSource(
        @Qualifier("batchDataSourceProperties") DataSourceProperties properties)

@Bean
@BatchTransactionManager
JdbcTransactionManager batchTransactionManager(
        @BatchDataSource DataSource batchDataSource)

@Bean
JdbcTemplate batchJdbcTemplate(@BatchDataSource DataSource batchDataSource)
```

Set `maximumPoolSize` to `1` and a descriptive pool name. Do not add
`@EnableBatchProcessing`; retaining Boot auto-configuration is required for
the official H2 Batch schema initializer.

- [ ] **Step 6: Configure SQLite and its schema**

`BusinessDataSourceConfiguration` exposes:

```java
@Bean
@ConfigurationProperties("app.datasource.business")
DataSourceProperties businessDataSourceProperties()

@Bean
@Primary
HikariDataSource businessDataSource(
        @Qualifier("businessDataSourceProperties") DataSourceProperties properties)

@Bean
@Primary
JdbcTransactionManager businessTransactionManager(
        @Qualifier("businessDataSource") DataSource businessDataSource)

@Bean
JdbcTemplate businessJdbcTemplate(
        @Qualifier("businessDataSource") DataSource businessDataSource)

@Bean
DataSourceScriptDatabaseInitializer businessDataSourceInitializer(
        @Qualifier("businessDataSource") DataSource businessDataSource)
```

Set the SQLite pool size to `1`. Configure the initializer with
`DatabaseInitializationMode.ALWAYS` and
`classpath:business-schema-sqlite.sql`.

Use this schema:

```sql
CREATE TABLE IF NOT EXISTS people (
    person_id INTEGER PRIMARY KEY AUTOINCREMENT,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    CONSTRAINT uk_people_name UNIQUE (first_name, last_name)
);
```

- [ ] **Step 7: Run the isolation and context tests**

```bash
GRADLE_USER_HOME=/tmp/spring-batch-modernization-gradle ./gradlew test --tests '*DataSourceIsolationTest' --tests '*BatchApplicationTests'
```

Expected: both tests pass; the test logs show temporary file paths rather than `./data/`.

- [ ] **Step 8: Commit the database boundary**

```bash
git add src/main/java/com/gsshop/batch/config src/main/resources/application.yml src/main/resources/business-schema-sqlite.sql src/test/resources/application.yml src/test/java/com/gsshop/batch/support src/test/java/com/gsshop/batch/config
git add -u src/main/resources/schema-all.sql
git commit -m "feat: separate Batch metadata and business databases"
```

---

### Task 3: Make person transformation immutable and validated

**Files:**

- Replace: `src/main/java/com/gsshop/batch/work/Person.java`
- Replace: `src/main/java/com/gsshop/batch/work/PersonItemProcessor.java`
- Create: `src/test/java/com/gsshop/batch/work/PersonItemProcessorTest.java`

**Interfaces:**

- Produces: `record Person(String firstName, String lastName)`.
- Produces: `PersonItemProcessor.process(Person): Person`, throwing `IllegalArgumentException` for blank fields.
- Consumes: Spring Batch 6 `org.springframework.batch.infrastructure.item.ItemProcessor`.

- [ ] **Step 1: Write failing processor tests**

```java
class PersonItemProcessorTest {

    private final PersonItemProcessor processor = new PersonItemProcessor();

    @Test
    void convertsNamesToUppercaseWithLocaleIndependentRules() throws Exception {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertThat(processor.process(new Person("Jill", "Doe")))
                    .isEqualTo(new Person("JILL", "DOE"));
        }
        finally {
            Locale.setDefault(original);
        }
    }

    @ParameterizedTest
    @MethodSource("invalidPeople")
    void rejectsBlankNames(Person person, String field) {
        assertThatThrownBy(() -> processor.process(person))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(field + " must not be blank");
    }

    static Stream<Arguments> invalidPeople() {
        return Stream.of(
                Arguments.of(new Person("", "Doe"), "firstName"),
                Arguments.of(new Person(" ", "Doe"), "firstName"),
                Arguments.of(new Person("Jill", ""), "lastName"),
                Arguments.of(new Person("Jill", " "), "lastName"));
    }
}
```

- [ ] **Step 2: Run the unit test to verify it fails**

```bash
GRADLE_USER_HOME=/tmp/spring-batch-modernization-gradle ./gradlew test --tests '*PersonItemProcessorTest'
```

Expected: compilation failure while `Person` is still a mutable class or assertion failure because blank values are accepted and default-locale uppercase is used.

- [ ] **Step 3: Implement the record and processor**

Use:

```java
public record Person(String firstName, String lastName) {
}
```

The processor must:

```java
public Person process(Person person) {
    String firstName = requireText(person.firstName(), "firstName");
    String lastName = requireText(person.lastName(), "lastName");
    Person transformed = new Person(
            firstName.toUpperCase(Locale.ROOT),
            lastName.toUpperCase(Locale.ROOT));
    log.info("Converting {} into {}", person, transformed);
    return transformed;
}
```

`requireText` uses `String.isBlank()` and throws exactly
`IllegalArgumentException("<field> must not be blank")`.

- [ ] **Step 4: Run the unit test**

```bash
GRADLE_USER_HOME=/tmp/spring-batch-modernization-gradle ./gradlew test --tests '*PersonItemProcessorTest'
```

Expected: all processor tests pass.

- [ ] **Step 5: Commit the domain update**

```bash
git add src/main/java/com/gsshop/batch/work/Person.java src/main/java/com/gsshop/batch/work/PersonItemProcessor.java src/test/java/com/gsshop/batch/work/PersonItemProcessorTest.java
git commit -m "feat: validate and normalize imported people"
```

---

### Task 4: Build the successful Spring Batch 6 import pipeline

**Files:**

- Delete: `src/main/java/com/gsshop/batch/work/BatchConfiguration.java`
- Create: `src/main/java/com/gsshop/batch/work/PersonImportJobConfiguration.java`
- Replace: `src/main/java/com/gsshop/batch/work/JobCompletionNotificationListener.java`
- Create: `src/test/java/com/gsshop/batch/work/PersonImportJobTest.java`
- Normalize: `src/main/resources/sample-data.csv`

**Interfaces:**

- Produces: `Job personImportJob`.
- Produces: `Step importPeopleStep` using `businessTransactionManager`.
- Produces: idempotent `JdbcBatchItemWriter<Person>` with SQLite upsert.
- Consumes: `JobRepository`, `businessDataSource`, `businessTransactionManager`, `businessJdbcTemplate`.

- [ ] **Step 1: Write the failing end-to-end success test**

Use `@SpringBatchTest` and `@SpringBootTest`:

```java
@SpringBatchTest
@SpringBootTest
class PersonImportJobTest extends BatchIntegrationTestSupport {

    @Autowired
    JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    @Qualifier("businessJdbcTemplate")
    JdbcTemplate businessJdbcTemplate;

    @Test
    void importsFiveUppercasePeople() throws Exception {
        JobExecution execution = jobOperatorTestUtils.startJob(
                jobOperatorTestUtils.getUniqueJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(businessJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM people", Integer.class)).isEqualTo(5);
        assertThat(businessJdbcTemplate.queryForList(
                "SELECT first_name FROM people ORDER BY person_id", String.class))
                .containsExactly("JILL", "JOE", "JUSTIN", "JANE", "JOHN");
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
GRADLE_USER_HOME=/tmp/spring-batch-modernization-gradle ./gradlew test --tests '*PersonImportJobTest'
```

Expected: compile or context failure because the old Batch 4 builders and item packages are incompatible with Spring Batch 6.

- [ ] **Step 3: Implement reader and SQLite writer**

In `PersonImportJobConfiguration`, create a reader with Spring Batch 6 imports
from `org.springframework.batch.infrastructure.item.*`. Map the record
explicitly:

```java
return new FlatFileItemReaderBuilder<Person>()
        .name("personItemReader")
        .resource(new ClassPathResource("sample-data.csv"))
        .delimited(spec -> spec.names("firstName", "lastName"))
        .fieldSetMapper(fieldSet -> new Person(
                fieldSet.readString("firstName"),
                fieldSet.readString("lastName")))
        .build();
```

Use a prepared-statement writer so record property introspection is not
implicit:

```java
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
```

- [ ] **Step 4: Implement the Spring Batch 6 Step and Job**

Construct the Step without deprecated factories or deprecated
`chunk(int, transactionManager)`:

```java
return new StepBuilder("importPeopleStep", jobRepository)
        .<Person, Person>chunk(10)
        .transactionManager(businessTransactionManager)
        .reader(reader)
        .processor(processor)
        .writer(writer)
        .build();
```

Construct `personImportJob` with `JobBuilder`, `RunIdIncrementer`, the
completion listener, and `importPeopleStep`. At this task, the default
`demo.failure=false` path contains only the import Step.

- [ ] **Step 5: Replace the listener**

Implement `JobExecutionListener` directly. It receives the qualified business
`JdbcTemplate`, logs the final status for every execution, and on
`BatchStatus.COMPLETED` logs:

```sql
SELECT COUNT(*) FROM people
```

Use parameterized SLF4J messages. Do not extend the removed legacy support
class and do not query an unqualified `JdbcTemplate`.

- [ ] **Step 6: Run the successful Job test**

```bash
GRADLE_USER_HOME=/tmp/spring-batch-modernization-gradle ./gradlew test --tests '*PersonImportJobTest'
```

Expected: Job status `COMPLETED` and five uppercase rows.

- [ ] **Step 7: Commit the working pipeline**

```bash
git add src/main/java/com/gsshop/batch/work src/main/resources/sample-data.csv src/test/java/com/gsshop/batch/work/PersonImportJobTest.java
git add -u src/main/java/com/gsshop/batch/work/BatchConfiguration.java
git commit -m "feat: import people into SQLite with Spring Batch 6"
```

---

### Task 5: Add deliberate failure and repeat-run guarantees

**Files:**

- Modify: `src/main/java/com/gsshop/batch/work/PersonImportJobConfiguration.java`
- Modify: `src/test/java/com/gsshop/batch/work/PersonImportJobTest.java`
- Create: `src/test/java/com/gsshop/batch/work/FailureDemoJobTest.java`
- Modify: `src/test/java/com/gsshop/batch/config/DataSourceIsolationTest.java`

**Interfaces:**

- Produces: `Step failureDemoStep`.
- Produces: `IllegalStateException("Intentional failure requested by demo.failure")`.
- Consumes: `demo.failure` boolean and the idempotent SQLite writer.

- [ ] **Step 1: Add a failing repeat-run test**

Add to `PersonImportJobTest`:

```java
@Test
void repeatedInstancesKeepFiveBusinessRowsAndAddBatchHistory() throws Exception {
    JobExecution first = jobOperatorTestUtils.startJob(
            jobOperatorTestUtils.getUniqueJobParameters());
    JobExecution second = jobOperatorTestUtils.startJob(
            jobOperatorTestUtils.getUniqueJobParameters());

    assertThat(first.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(second.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(businessJdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM people", Integer.class)).isEqualTo(5);
}
```

Also query the qualified Batch `JdbcTemplate` and assert that
`SELECT COUNT(*) FROM BATCH_JOB_EXECUTION` increases by two.

- [ ] **Step 2: Add the failing failure-demo test**

Create a separate context:

```java
@SpringBatchTest
@SpringBootTest(properties = "demo.failure=true")
class FailureDemoJobTest extends BatchIntegrationTestSupport {

    @Autowired
    JobOperatorTestUtils jobOperatorTestUtils;

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
```

- [ ] **Step 3: Run the new tests to verify the failure-demo test fails**

```bash
GRADLE_USER_HOME=/tmp/spring-batch-modernization-gradle ./gradlew test --tests '*PersonImportJobTest' --tests '*FailureDemoJobTest'
```

Expected: repeat-run test passes because of upsert; failure-demo assertion fails because no intentional failure Step exists yet.

- [ ] **Step 4: Implement the optional failure Step**

Bind the option with:

```java
@Value("${demo.failure:false}")
boolean failureEnabled
```

Build the Step using the non-deprecated tasklet form:

```java
return new StepBuilder("failureDemoStep", jobRepository)
        .tasklet((contribution, chunkContext) -> {
            throw new IllegalStateException(
                    "Intentional failure requested by demo.failure");
        })
        .transactionManager(businessTransactionManager)
        .build();
```

In the Job bean, create the `SimpleJobBuilder` with `importPeopleStep` and add
`failureDemoStep` only when `failureEnabled` is true. Do not mark a successful
tasklet with a failed exit status; throw the explicit exception so the root
cause is inspectable.

- [ ] **Step 5: Complete database isolation assertions**

Inject the `batchJdbcTemplate` bean created in Task 2 with
`@Qualifier("batchJdbcTemplate")`. Assert the H2 execution table grows while
SQLite contains no `BATCH_%` tables.

- [ ] **Step 6: Run all application tests**

```bash
GRADLE_USER_HOME=/tmp/spring-batch-modernization-gradle ./gradlew clean test
```

Expected: all unit and integration tests pass; no test creates or changes a
file below the repository's `data/` directory.

- [ ] **Step 7: Commit failure and repeat behavior**

```bash
git add src/main/java/com/gsshop/batch/work/PersonImportJobConfiguration.java src/test/java/com/gsshop/batch/work/PersonImportJobTest.java src/test/java/com/gsshop/batch/work/FailureDemoJobTest.java src/test/java/com/gsshop/batch/config/DataSourceIsolationTest.java
git commit -m "test: cover failure and repeatable Batch executions"
```

---

### Task 6: Replace obsolete documentation and add maintenance automation

**Files:**

- Replace: `README.md`
- Create: `CONTRIBUTING.md`
- Create: `.github/workflows/build.yml`
- Create: `.github/workflows/dependency-submission.yml`
- Create: `.github/dependabot.yml`
- Replace: `src/test/java/com/gsshop/batch/BatchApplicationTests.java`

**Interfaces:**

- Produces: verified copy-paste commands for normal, repeated, failing, and reset scenarios.
- Produces: CI contract `./gradlew clean test --no-daemon`.
- Consumes: all verified runtime and test behavior from Tasks 1–5.

- [ ] **Step 1: Keep the smoke test explicit**

Replace `BatchApplicationTests` with a test that extends
`BatchIntegrationTestSupport`, uses `@SpringBootTest`, and asserts the context
provides exactly two `DataSource` beans and exactly two
`PlatformTransactionManager` beans. The test profile already prevents Job
auto-launch.

- [ ] **Step 2: Verify commands before documenting them**

Run normal execution with a unique Batch parameter:

```bash
mkdir -p data
GRADLE_USER_HOME=/tmp/spring-batch-modernization-gradle ./gradlew bootRun --args='run.id=1,java.lang.Long,true'
```

Run a second successful instance:

```bash
GRADLE_USER_HOME=/tmp/spring-batch-modernization-gradle ./gradlew bootRun --args='run.id=2,java.lang.Long,true'
```

Run the failure demonstration:

```bash
GRADLE_USER_HOME=/tmp/spring-batch-modernization-gradle ./gradlew bootRun --args='--demo.failure=true run.id=3,java.lang.Long,true'
```

Expected: first two commands finish `COMPLETED`, the third reports `FAILED`,
and logs show a stable business row count of five.

- [ ] **Step 3: Rewrite README around the learning path**

The README must contain, in this order:

1. English summary.
2. Three-command quick start.
3. Korean purpose and prerequisites.
4. Mermaid diagram showing CSV → processing → SQLite and Job metadata → H2.
5. Exact bean and transaction-manager mapping.
6. Normal, repeat, and failure commands verified in Step 1.
7. Explanation of idempotent upsert and the absence of cross-database atomicity.
8. Local reset command that targets only `./data/`.
9. Spring Boot 2.4 / Batch 4 → Boot 4.1 / Batch 6 migration table.
10. Troubleshooting for JDK version, locked DB files, and Windows paths.
11. Security note stating that removal from the tree does not erase the old credential from Git history.
12. Links to the authoritative Spring Boot, Spring Batch, Gradle, H2, and Xerial SQLite JDBC documentation.

Do not retain obsolete Initializr screenshots as the primary instructions.

- [ ] **Step 4: Add contributor documentation**

`CONTRIBUTING.md` must require:

- JDK 21;
- use of the checked-in wrapper;
- `./gradlew clean test` before a pull request;
- tests using temporary database files;
- no secrets or machine-specific database paths;
- updates to README commands when runtime behavior changes.

- [ ] **Step 5: Add the build workflow**

Create `.github/workflows/build.yml`:

```yaml
name: Build

on:
  push:
    branches: [main]
  pull_request:

permissions:
  contents: read

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v6
      - uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: "21"
      - uses: gradle/actions/setup-gradle@v6
      - run: ./gradlew clean test --no-daemon
```

`setup-gradle@v6` performs wrapper validation by default.

- [ ] **Step 6: Add dependency submission and Dependabot**

Create a push-only dependency submission workflow:

```yaml
name: Dependency Submission

on:
  push:
    branches: [main]

permissions:
  contents: write

jobs:
  submit:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v6
      - uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: "21"
      - uses: gradle/actions/dependency-submission@v6
```

Create `.github/dependabot.yml` with version `2` and two weekly entries:

```yaml
version: 2
updates:
  - package-ecosystem: gradle
    directory: /
    schedule:
      interval: weekly
  - package-ecosystem: github-actions
    directory: /
    schedule:
      interval: weekly
```

- [ ] **Step 7: Verify the working tree no longer contains the exposed connection**

Run:

```bash
git grep -n -E '10[.]52[.]181[.]241|Batch!!|mysql://|oracle:thin'
```

Expected: no output. Do not rewrite Git history in this task. Record in the
handoff that the historical credential must be revoked or rotated.

- [ ] **Step 8: Run final local verification**

```bash
GRADLE_USER_HOME=/tmp/spring-batch-modernization-gradle ./gradlew clean test --no-daemon
GRADLE_USER_HOME=/tmp/spring-batch-modernization-gradle ./gradlew dependencies --configuration runtimeClasspath
git diff --check
git status --short
```

Expected:

- all tests pass;
- the resolved runtime graph uses Spring Batch 6.0.4;
- no unexpected JPA, Oracle, or MySQL dependency is present;
- `git diff --check` produces no output;
- only the intended documentation and automation files remain uncommitted.

- [ ] **Step 9: Commit documentation and automation**

```bash
git add README.md CONTRIBUTING.md .github src/test/java/com/gsshop/batch/BatchApplicationTests.java
git commit -m "docs: publish the multi-datasource learning guide"
```

---

### Task 7: Final acceptance and security handoff

**Files:**

- Modify only if verification exposes a defect: files owned by Tasks 1–6.
- No new production feature files.

**Interfaces:**

- Consumes: every success criterion in the approved design.
- Produces: an evidence-backed final report; no unverified success claims.

- [ ] **Step 1: Start from a clean local demo state**

Remove only generated database files below the repository's explicit data
directory, preserving the directory itself if present:

```bash
find ./data -mindepth 1 -maxdepth 1 -type f -delete
```

If `./data/` does not exist, create it with `mkdir -p data`. Do not use a
recursive deletion command.

- [ ] **Step 2: Execute the acceptance sequence**

```bash
GRADLE_USER_HOME=/tmp/spring-batch-modernization-gradle ./gradlew clean test --no-daemon
GRADLE_USER_HOME=/tmp/spring-batch-modernization-gradle ./gradlew bootRun --args='run.id=101,java.lang.Long,true'
GRADLE_USER_HOME=/tmp/spring-batch-modernization-gradle ./gradlew bootRun --args='run.id=102,java.lang.Long,true'
```

Expected: tests pass, both Jobs complete, H2 and SQLite files appear only
below `./data/`, and the second run still reports five people.

- [ ] **Step 3: Execute and inspect the failure demo**

```bash
GRADLE_USER_HOME=/tmp/spring-batch-modernization-gradle ./gradlew bootRun --args='--demo.failure=true run.id=103,java.lang.Long,true'
```

Expected: process reports the deliberate failure and includes
`Intentional failure requested by demo.failure`; imported business rows remain
five.

- [ ] **Step 4: Audit dependencies and secret removal**

```bash
GRADLE_USER_HOME=/tmp/spring-batch-modernization-gradle ./gradlew dependencyInsight --dependency spring-batch-core --configuration runtimeClasspath
GRADLE_USER_HOME=/tmp/spring-batch-modernization-gradle ./gradlew dependencyInsight --dependency sqlite-jdbc --configuration runtimeClasspath
git grep -n -E '10[.]52[.]181[.]241|Batch!!|mysql://|oracle:thin'
shasum -a 256 gradle/wrapper/gradle-wrapper.jar
```

Expected:

- Spring Batch resolves to 6.0.4;
- SQLite JDBC resolves to 3.53.2.0;
- the secret search has no matches;
- wrapper JAR checksum is
  `497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7`.

After push, inspect the GitHub dependency graph and Dependabot alerts. Report
that status separately from the local dependency-resolution evidence; do not
claim a remote advisory scan ran if it did not.

- [ ] **Step 5: Confirm repository cleanliness**

```bash
git diff --check
git status --short
git log --oneline -8
```

Expected: no uncommitted implementation changes and a focused commit for each
task boundary.

- [ ] **Step 6: Prepare the final handoff**

Report:

- exact versions;
- test count and command output summary;
- normal, repeated, and failure-demo results;
- dependency and wrapper checksum evidence;
- the need to revoke or rotate the credential present in old Git history;
- the need for the owner to choose a license before encouraging unrestricted reuse.
