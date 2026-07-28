package com.gsshop.batch.work;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.infrastructure.item.ItemProcessor;

public class PersonItemProcessor implements ItemProcessor<Person, Person> {

    private static final Logger log = LoggerFactory.getLogger(PersonItemProcessor.class);

    @Override
    public Person process(Person person) {
        String firstName = requireText(person.firstName(), "firstName");
        String lastName = requireText(person.lastName(), "lastName");
        Person transformed = new Person(
                firstName.toUpperCase(Locale.ROOT),
                lastName.toUpperCase(Locale.ROOT));
        log.info("Converting {} into {}", person, transformed);
        return transformed;
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
