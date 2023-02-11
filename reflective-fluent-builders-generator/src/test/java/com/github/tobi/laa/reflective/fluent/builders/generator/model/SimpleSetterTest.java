package com.github.tobi.laa.reflective.fluent.builders.generator.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleSetterTest {

    @ParameterizedTest
    @ValueSource(strings = {"otherName", "yetAnotherName"})
    void testWithParamName(final String paramName) {
        // Arrange
        final var simpleSetter = SimpleSetter.builder()
                .methodName("getSth")
                .paramType(Map.class)
                .paramName("aName")
                .visibility(Visibility.PRIVATE)
                .build();
        // Act
        final var withParamName = simpleSetter.withParamName(paramName);
        // Assert
        assertThat(withParamName).usingRecursiveComparison().isEqualTo(SimpleSetter.builder()
                .methodName("getSth")
                .paramType(Map.class)
                .paramName(paramName)
                .visibility(Visibility.PRIVATE)
                .build());
    }
}