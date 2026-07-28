package com.gsshop.batch.work;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
