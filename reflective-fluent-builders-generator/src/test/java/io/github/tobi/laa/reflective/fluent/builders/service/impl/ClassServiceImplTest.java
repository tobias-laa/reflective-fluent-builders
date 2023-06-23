package io.github.tobi.laa.reflective.fluent.builders.service.impl;

import com.google.common.reflect.ClassPath;
import io.github.tobi.laa.reflective.fluent.builders.exception.ReflectionException;
import io.github.tobi.laa.reflective.fluent.builders.props.api.BuildersProperties;
import io.github.tobi.laa.reflective.fluent.builders.test.models.complex.hierarchy.*;
import io.github.tobi.laa.reflective.fluent.builders.test.models.complex.hierarchy.second.SecondSuperClassInDifferentPackage;
import io.github.tobi.laa.reflective.fluent.builders.test.models.nested.NestedMarker;
import io.github.tobi.laa.reflective.fluent.builders.test.models.nested.TopLevelClass;
import io.github.tobi.laa.reflective.fluent.builders.test.models.simple.*;
import io.github.tobi.laa.reflective.fluent.builders.test.models.simple.hierarchy.Child;
import io.github.tobi.laa.reflective.fluent.builders.test.models.simple.hierarchy.Parent;
import lombok.SneakyThrows;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassServiceImplTest {

    private ClassServiceImpl classServiceImpl;

    @Mock
    private BuildersProperties properties;

    @BeforeEach
    void init() {
        classServiceImpl = new ClassServiceImpl(properties, ClassLoader.getSystemClassLoader());
    }

    @Test
    void testCollectFullClassHierarchyNull() {
        // Act
        final Executable collectFullClassHierarchy = () -> classServiceImpl.collectFullClassHierarchy(null);
        // Assert
        assertThrows(NullPointerException.class, collectFullClassHierarchy);
    }

    @ParameterizedTest
    @MethodSource
    void testCollectFullClassHierarchy(final Class<?> clazz, final Set<Predicate<Class<?>>> excludes, final List<Class<?>> expected) {
        // Arrange
        final var hierarchyCollection = Mockito.mock(BuildersProperties.HierarchyCollection.class);
        when(properties.getHierarchyCollection()).thenReturn(hierarchyCollection);
        when(hierarchyCollection.getExcludes()).thenReturn(excludes);
        // Act
        final List<Class<?>> actual = classServiceImpl.collectFullClassHierarchy(clazz);
        // Assert
        assertEquals(expected, actual);
    }

    private static Stream<Arguments> testCollectFullClassHierarchy() {
        return Stream.of( //
                Arguments.of( //
                        ClassWithHierarchy.class, //
                        Collections.emptySet(), //
                        List.of( //
                                ClassWithHierarchy.class, //
                                AnInterface.class, //
                                FirstSuperClass.class, //
                                SecondSuperClassInDifferentPackage.class, //
                                TopLevelSuperClass.class, //
                                AnotherInterface.class, //
                                Object.class)), //
                Arguments.of( //
                        ClassWithHierarchy.class, //
                        Set.<Predicate<Class<?>>>of(Object.class::equals), //
                        List.of( //
                                ClassWithHierarchy.class, //
                                AnInterface.class, //
                                FirstSuperClass.class, //
                                SecondSuperClassInDifferentPackage.class, //
                                TopLevelSuperClass.class, //
                                AnotherInterface.class)), //
                Arguments.of( //
                        ClassWithHierarchy.class, //
                        Set.<Predicate<Class<?>>>of(Object.class::equals, AnInterface.class::equals), //
                        List.of( //
                                ClassWithHierarchy.class, //
                                FirstSuperClass.class, //
                                SecondSuperClassInDifferentPackage.class, //
                                TopLevelSuperClass.class, //
                                AnotherInterface.class)), //
                Arguments.of( //
                        ClassWithHierarchy.class, //
                        Set.<Predicate<Class<?>>>of(FirstSuperClass.class::equals), //
                        List.of( //
                                ClassWithHierarchy.class, //
                                AnInterface.class)));
    }

    @Test
    void testCollectClassesRecursivelyNull() {
        // Arrange
        final String packageName = null;
        // Act
        final Executable collectClassesRecursively = () -> classServiceImpl.collectClassesRecursively(packageName);
        // Assert
        assertThrows(NullPointerException.class, collectClassesRecursively);
    }

    @Test
    void testCollectClassesRecursivelyReflectionException() {
        try (final MockedStatic<ClassPath> classPath = mockStatic(ClassPath.class)) {
            // Arrange
            final var cause = new IOException("Thrown in unit test");
            classPath.when(() -> ClassPath.from(any())).thenThrow(cause);
            // Act
            final ThrowingCallable collectClassesRecursively = () -> classServiceImpl.collectClassesRecursively("");
            // Assert
            assertThatThrownBy(collectClassesRecursively)
                    .isInstanceOf(ReflectionException.class)
                    .hasMessage("Error while attempting to collect classes recursively.")
                    .hasCause(cause);
        }
    }

    @ParameterizedTest
    @MethodSource
    void testCollectClassesRecursively(final String packageName, final Set<Class<?>> expected) {
        // Act
        final Set<Class<?>> actual = classServiceImpl.collectClassesRecursively(packageName);
        // Assert
        assertThat(actual).filteredOn(not(this::isTestClass)).containsExactlyInAnyOrderElementsOf(expected);
    }

    private boolean isTestClass(final Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredMethods())
                .map(Method::getDeclaredAnnotations)
                .flatMap(Arrays::stream)
                .map(Annotation::annotationType)
                .anyMatch(Set.of(Test.class, ParameterizedTest.class)::contains);
    }

    @SneakyThrows
    private static Stream<Arguments> testCollectClassesRecursively() {
        return Stream.of( //
                Arguments.of(
                        Simple.class.getPackageName(), //
                        Set.of( //
                                Child.class, //
                                Parent.class, //
                                Simple.class, //
                                SimpleAbstractClass.class, //
                                SimpleClass.class, //
                                SimpleClassNoDefaultConstructor.class, //
                                SimpleClassNoSetPrefix.class)),
                Arguments.of(
                        NestedMarker.class.getPackageName(), //
                        Set.of( //
                                NestedMarker.class, //
                                TopLevelClass.class, //
                                TopLevelClass.NestedPublicLevelOne.class, //
                                Class.forName(TopLevelClass.class.getName() + "$NestedProtectedLevelOne"), //
                                Class.forName(TopLevelClass.class.getName() + "$NestedPackagePrivateLevelOne"), //
                                Class.forName(TopLevelClass.class.getName() + "$NestedPrivateLevelOne"), //
                                TopLevelClass.NestedNonStatic.class, //
                                TopLevelClass.NestedPublicLevelOne.NestedPublicLevelTwo.class, //
                                TopLevelClass.NestedPublicLevelOne.NestedPublicLevelTwo.NestedPublicLevelThree.class)));
    }

    @Test
    void testDetermineClassLocationNull() {
        // Arrange
        final Class<?> clazz = null;
        // Act
        final Executable determineClassLocation = () -> classServiceImpl.determineClassLocation(clazz);
        // Assert
        assertThrows(NullPointerException.class, determineClassLocation);
    }

    @Test
    void testDetermineClassLocationCodeSourceNull() {
        // Arrange
        final var clazz = String.class;
        // Act
        final Optional<Path> actual = classServiceImpl.determineClassLocation(clazz);
        // Assert
        assertThat(actual).isEmpty();
    }

    @Test
    void testDetermineClassLocationFromJar() {
        // Arrange
        final var clazz = Test.class;
        // Act
        final Optional<Path> actual = classServiceImpl.determineClassLocation(clazz);
        // Assert
        assertThat(actual).isPresent();
        assertThat(actual.get()).isRegularFile().hasExtension("jar");
    }

    @Test
    void testDetermineClassLocationFromClassFile() {
        // Arrange
        final var clazz = getClass();
        // Act
        final Optional<Path> actual = classServiceImpl.determineClassLocation(clazz);
        // Assert
        assertThat(actual).isPresent();
        assertThat(actual.get()).isRegularFile().hasExtension("class");
    }

    @Test
    @SneakyThrows
    void testGetLocationAsPathURISyntaxException() {
        // Arrange
        final var codeSource = Mockito.mock(CodeSource.class);
        final var url = Mockito.mock(URL.class);
        when(codeSource.getLocation()).thenReturn(url);
        when(url.toURI()).thenThrow(new URISyntaxException("mock", "Thrown in unit test."));
        // Act
        final Executable getLocationAsPath = () -> classServiceImpl.getLocationAsPath(codeSource);
        // Assert
        assertThrows(URISyntaxException.class, getLocationAsPath);
    }

    @ParameterizedTest
    @MethodSource
    void testExistsOnClasspath(final String className, final boolean expected) {
        // Act
        final boolean actual = classServiceImpl.existsOnClasspath(className);
        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    private static Stream<Arguments> testExistsOnClasspath() {
        return Stream.of( //
                Arguments.of("this.class.exists.not", false), //
                Arguments.of("io.github.tobi.laa.reflective.fluent.builders.mojo.GenerateBuildersMojo", false), //
                Arguments.of("java.lang.String", true), //
                Arguments.of("io.github.tobi.laa.reflective.fluent.builders.service.api.ClassService", true));
    }
}