package io.github.tobi.laa.reflective.fluent.builders.service.api;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassGraphException;
import io.github.classgraph.ClassInfo;
import io.github.tobi.laa.reflective.fluent.builders.exception.ReflectionException;
import io.github.tobi.laa.reflective.fluent.builders.props.api.BuildersProperties;
import io.github.tobi.laa.reflective.fluent.builders.test.IntegrationTest;
import io.github.tobi.laa.reflective.fluent.builders.test.models.complex.hierarchy.*;
import io.github.tobi.laa.reflective.fluent.builders.test.models.complex.hierarchy.second.SecondSuperClassInDifferentPackage;
import io.github.tobi.laa.reflective.fluent.builders.test.models.nested.NestedMarker;
import io.github.tobi.laa.reflective.fluent.builders.test.models.nested.TopLevelClass;
import io.github.tobi.laa.reflective.fluent.builders.test.models.simple.*;
import io.github.tobi.laa.reflective.fluent.builders.test.models.simple.hierarchy.Child;
import io.github.tobi.laa.reflective.fluent.builders.test.models.simple.hierarchy.Parent;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.security.SecureClassLoader;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@IntegrationTest
class ClassServiceIT {

    @Inject
    private ClassService service;

    @MockBean
    private BuildersProperties properties;

    @Test
    void testCollectFullClassHierarchyNull() {
        // Act
        final Executable collectFullClassHierarchy = () -> service.collectFullClassHierarchy(null);
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
        final List<Class<?>> actual = service.collectFullClassHierarchy(clazz);
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
        final Executable collectClassesRecursively = () -> service.collectClassesRecursively(packageName);
        // Assert
        assertThrows(NullPointerException.class, collectClassesRecursively);
    }

    @SuppressWarnings("unused")
    @Test
    void testCollectClassesRecursivelyReflectionException() {
        // Arrange
        final var cause = classGraphException("Thrown in unit test");
        try (final var classGraph = mockConstruction(
                ClassGraph.class,
                withSettings().defaultAnswer(InvocationOnMock::getMock),
                (mock, ctx) -> {
                    doThrow(cause).when(mock).scan();
                })) {
            // Act
            final ThrowingCallable collectClassesRecursively = () -> service.collectClassesRecursively("");
            // Assert
            assertThatThrownBy(collectClassesRecursively)
                    .isInstanceOf(ReflectionException.class)
                    .hasMessage("Error while attempting to collect classes recursively.")
                    .hasCause(cause);
        }
    }

    @SneakyThrows
    private ClassGraphException classGraphException(final String message) {
        final var constructor = ClassGraphException.class.getDeclaredConstructor(String.class);
        constructor.setAccessible(true);
        return constructor.newInstance(message);
    }

    @ParameterizedTest
    @MethodSource
    void testCollectClassesRecursively(final String packageName, final Set<Class<?>> expected) {
        // Act
        final Set<Class<?>> actual = service.collectClassesRecursively(packageName);
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
        final Executable determineClassLocation = () -> service.determineClassLocation(clazz);
        // Assert
        assertThrows(NullPointerException.class, determineClassLocation);
    }

    @Test
    void testDetermineClassLocationCodeSourceNull() {
        // Arrange
        final var clazz = String.class;
        // Act
        final Optional<Path> actual = service.determineClassLocation(clazz);
        // Assert
        assertThat(actual).isEmpty();
    }

    @Test
    void testDetermineClassLocationFromJar() {
        // Arrange
        final var clazz = Test.class;
        // Act
        final Optional<Path> actual = service.determineClassLocation(clazz);
        // Assert
        assertThat(actual).isPresent();
        assertThat(actual.get()).isRegularFile().hasExtension("jar");
    }

    @Test
    void testDetermineClassLocationFromClassFile() {
        // Arrange
        final var clazz = getClass();
        // Act
        final Optional<Path> actual = service.determineClassLocation(clazz);
        // Assert
        assertThat(actual).isPresent();
        assertThat(actual.get()).isRegularFile().hasExtension("class");
    }

//    @Test
//    @SneakyThrows
//    void testGetLocationAsPathURISyntaxException() {
//        // Arrange
//        final var codeSource = Mockito.mock(CodeSource.class);
//        final var url = Mockito.mock(URL.class);
//        when(codeSource.getLocation()).thenReturn(url);
//        when(url.toURI()).thenThrow(new URISyntaxException("mock", "Thrown in unit test."));
//        // Act
//        final Executable getLocationAsPath = () -> service.getLocationAsPath(codeSource);
//        // Assert
//        assertThrows(URISyntaxException.class, getLocationAsPath);
//    }
//
//    @Test
//    void testLoadClassNull() {
//        // Arrange
//        final String className = null;
//        final var classLoader = spy(getSystemClassLoader());
//        service = new ClassServiceImpl(properties, () -> classLoader);
//        // Act
//        final ThrowingCallable loadClass = () -> service.loadClass(className);
//        // Assert
//        assertThatThrownBy(loadClass).isExactlyInstanceOf(NullPointerException.class);
//        verifyNoInteractions(classLoader);
//    }
//
//    @ParameterizedTest
//    @ValueSource(classes = {LinkageError.class, SecurityException.class})
//    @SneakyThrows
//    @Disabled
//    void testLoadClassException(final Class<? extends Throwable> causeType) {
//        // Arrange
//        final var className = "does.not.matter";
//        final var cause = causeType.getDeclaredConstructor(String.class).newInstance("Thrown in unit test.");
//        final var classLoader = new ThrowingClassLoader(cause);
//        service = new ClassServiceImpl(properties, () -> classLoader);
//        // Act
//        final ThrowingCallable loadClass = () -> service.loadClass(className);
//        // Assert
//        assertThatThrownBy(loadClass) //
//                .isExactlyInstanceOf(ReflectionException.class) //
//                .hasMessage("Error while attempting to load class does.not.matter.") //
//                .hasCause(cause);
//    }

    @ParameterizedTest
    @ValueSource(strings = {"this.class.exists.not", "io.github.tobi.laa.reflective.fluent.builders.mojo.GenerateBuildersMojo"})
    void testLoadClassEmpty(final String className) {
        // Act
        final Optional<ClassInfo> actual = service.loadClass(className);
        // Assert
        assertThat(actual).isEmpty();
    }

    @ParameterizedTest
    @MethodSource
    void testLoadClass(final String className, final Class<?> expected) {
        // Act
        final Optional<ClassInfo> actual = service.loadClass(className);
        // Assert
        assertThat(actual).get().hasFieldOrPropertyWithValue("name", expected.getName());
    }

    private static Stream<Arguments> testLoadClass() {
        return Stream.of( //
                Arguments.of("java.lang.String", String.class), //
                Arguments.of("io.github.tobi.laa.reflective.fluent.builders.service.api.ClassService", ClassService.class));
    }

    @Test
    void testIsAbstractNull() {
        // Arrange
        final Class<?> clazz = null;
        // Act
        final ThrowingCallable isAbstract = () -> service.isAbstract(clazz);
        // Assert
        assertThatThrownBy(isAbstract).isExactlyInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @MethodSource
    void testIsAbstract(final Class<?> clazz, final boolean expected) {
        // Act
        final boolean actual = service.isAbstract(clazz);
        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    private static Stream<Arguments> testIsAbstract() {
        return Stream.of( //
                Arguments.of(SimpleClass.class, false), //
                Arguments.of(SimpleAbstractClass.class, true));
    }

    @RequiredArgsConstructor
    private static class ThrowingClassLoader extends SecureClassLoader {

        private final Throwable exception;

        @SneakyThrows
        @Override
        public Class<?> loadClass(final String name) {
            throw exception;
        }
    }
}