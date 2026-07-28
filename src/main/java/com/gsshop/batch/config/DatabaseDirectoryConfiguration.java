package com.gsshop.batch.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DatabaseDirectoryConfiguration {

    @Bean
    Path databaseDirectory(@Value("${app.data-directory:./data}") Path directory) {
        try {
            return Files.createDirectories(directory.toAbsolutePath().normalize());
        }
        catch (IOException exception) {
            throw new IllegalStateException("Could not create database directory: " + directory, exception);
        }
    }
}
