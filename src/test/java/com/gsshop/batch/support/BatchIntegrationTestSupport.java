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
