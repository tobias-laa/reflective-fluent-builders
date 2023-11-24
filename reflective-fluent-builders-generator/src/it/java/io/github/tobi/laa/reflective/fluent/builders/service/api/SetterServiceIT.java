package io.github.tobi.laa.reflective.fluent.builders.service.api;

import io.github.tobi.laa.reflective.fluent.builders.model.*;
import io.github.tobi.laa.reflective.fluent.builders.props.api.BuildersProperties;
import io.github.tobi.laa.reflective.fluent.builders.test.InjectSpy;
import io.github.tobi.laa.reflective.fluent.builders.test.IntegrationTest;
import io.github.tobi.laa.reflective.fluent.builders.test.models.complex.ClassWithCollections;
import io.github.tobi.laa.reflective.fluent.builders.test.models.complex.GetAndAdd;
import io.github.tobi.laa.reflective.fluent.builders.test.models.complex.ListWithTwoParams;
import io.github.tobi.laa.reflective.fluent.builders.test.models.complex.MapWithThreeParams;
import io.github.tobi.laa.reflective.fluent.builders.test.models.complex.hierarchy.*;
import io.github.tobi.laa.reflective.fluent.builders.test.models.complex.hierarchy.generics.Generic;
import io.github.tobi.laa.reflective.fluent.builders.test.models.complex.hierarchy.generics.GenericChild;
import io.github.tobi.laa.reflective.fluent.builders.test.models.complex.hierarchy.generics.GenericGrandChild;
import io.github.tobi.laa.reflective.fluent.builders.test.models.complex.hierarchy.generics.GenericParent;
import io.github.tobi.laa.reflective.fluent.builders.test.models.complex.hierarchy.second.SecondSuperClassInDifferentPackage;
import io.github.tobi.laa.reflective.fluent.builders.test.models.jaxb.PersonJaxb;
import io.github.tobi.laa.reflective.fluent.builders.test.models.jaxb.PetJaxb;
import io.github.tobi.laa.reflective.fluent.builders.test.models.simple.SimpleClass;
import io.github.tobi.laa.reflective.fluent.builders.test.models.simple.SimpleClassNoSetPrefix;
import lombok.SneakyThrows;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.inject.Inject;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;
import java.util.stream.Stream;

import static io.github.tobi.laa.reflective.fluent.builders.model.Visibility.*;
import static org.apache.commons.lang3.reflect.TypeUtils.parameterize;
import static org.apache.commons.lang3.reflect.TypeUtils.wildcardType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@IntegrationTest
class SetterServiceIT {

    @Inject
    private SetterService setterService;

    @InjectSpy
    private BuildersProperties properties;

    @Test
    void testDropSetterPrefixNull() {
        // Act
        final Executable dropSetterPrefix = () -> setterService.dropSetterPrefix(null);
        // Assert
        assertThrows(NullPointerException.class, dropSetterPrefix);
    }

    @ParameterizedTest
    @MethodSource
    void testDropSetterPrefix(final String setterPrefix, final String name, final String expected) {
        // Arrange
        when(properties.getSetterPrefix()).thenReturn(setterPrefix);
        // Act
        final String actual = setterService.dropSetterPrefix(name);
        // Assert
        assertEquals(expected, actual);
    }

    private static Stream<Arguments> testDropSetterPrefix() {
        return Stream.of( //
                Arguments.of("set", "set", "set"), //
                Arguments.of("set", "setAge", "age"), //
                Arguments.of("set", "withAge", "withAge"), //
                Arguments.of("set", "setSetAge", "setAge"));
    }

    @Test
    void testDropGetterPrefixNull() {
        // Act
        final Executable dropGetterPrefix = () -> setterService.dropGetterPrefix(null);
        // Assert
        assertThrows(NullPointerException.class, dropGetterPrefix);
    }

    @ParameterizedTest
    @MethodSource
    void testDropGetterPrefix(final String getterPrefix, final String name, final String expected) {
        // Arrange
        when(properties.getGetterPrefix()).thenReturn(getterPrefix);
        // Act
        final String actual = setterService.dropGetterPrefix(name);
        // Assert
        assertEquals(expected, actual);
    }

    private static Stream<Arguments> testDropGetterPrefix() {
        return Stream.of( //
                Arguments.of("get", "get", "get"), //
                Arguments.of("get", "getAge", "age"), //
                Arguments.of("get", "withAge", "withAge"), //
                Arguments.of("get", "getGetAge", "getAge"));
    }

    @Test
    void testGatherAllSettersNull() {
        // Act
        final Executable gatherAllSetters = () -> setterService.gatherAllSetters(null);
        // Assert
        assertThrows(NullPointerException.class, gatherAllSetters);
    }

    @ParameterizedTest
    @MethodSource
    void testGatherAllSetters(final String setterPrefix, final String getterPrefix, final boolean getAndAddEnabled, final Class<?> clazz, final Set<Setter> expected) {
        // Arrange
        when(properties.getSetterPrefix()).thenReturn(setterPrefix);
        when(properties.isGetAndAddEnabled()).thenReturn(getAndAddEnabled);
        if (getAndAddEnabled) {
            when(properties.getGetterPrefix()).thenReturn(getterPrefix);
        }
        // Act
        final Set<Setter> actual = setterService.gatherAllSetters(clazz);
        // Assert
        assertThat(actual)
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withEqualsForFields(
                                (a, b) -> ((Type) a).getTypeName().equals(((Type) b).getTypeName()),
                                "paramType", "paramTypeArg", "keyType", "valueType")
                        .build())
                .isEqualTo(expected);
    }

    @SneakyThrows
    private static Stream<Arguments> testGatherAllSetters() {
        final var setOfList = ClassWithCollections.class.getDeclaredField("set").getGenericType();
        return Stream.of( //
                Arguments.of("set", null, false, SimpleClass.class, //
                        Set.of( //
                                SimpleSetter.builder().methodName("setAnInt").paramName("anInt").paramType(int.class).visibility(PUBLIC).declaringClass(SimpleClass.class).build(), //
                                SimpleSetter.builder().methodName("setAString").paramName("aString").paramType(String.class).visibility(PUBLIC).declaringClass(SimpleClass.class).build(), //
                                SimpleSetter.builder().methodName("setBooleanField").paramName("booleanField").paramType(boolean.class).visibility(PUBLIC).declaringClass(SimpleClass.class).build(), //
                                SimpleSetter.builder().methodName("setSetClass").paramName("setClass").paramType(parameterize(Class.class, wildcardType().withUpperBounds(Object.class).build())).visibility(PUBLIC).declaringClass(SimpleClass.class).build())), //
                Arguments.of("", null, false, SimpleClassNoSetPrefix.class, //
                        Set.of( //
                                SimpleSetter.builder().methodName("anInt").paramName("anInt").paramType(int.class).visibility(PACKAGE_PRIVATE).declaringClass(SimpleClassNoSetPrefix.class).build(), //
                                SimpleSetter.builder().methodName("aString").paramName("aString").paramType(String.class).visibility(PACKAGE_PRIVATE).declaringClass(SimpleClassNoSetPrefix.class).build())), //
                Arguments.of("set", "get", false, ClassWithCollections.class, //
                        Set.of( //
                                CollectionSetter.builder().methodName("setInts").paramName("ints").paramType(parameterize(Collection.class, Integer.class)).paramTypeArg(Integer.class).visibility(PUBLIC).declaringClass(ClassWithCollections.class).build(), //
                                CollectionSetter.builder().methodName("setList").paramName("list").paramType(List.class).paramTypeArg(Object.class).visibility(PUBLIC).declaringClass(ClassWithCollections.class).build(), //
                                CollectionSetter.builder().methodName("setSet").paramName("set").paramType(setOfList).paramTypeArg(List.class).visibility(PUBLIC).declaringClass(ClassWithCollections.class).build(), //
                                CollectionSetter.builder().methodName("setDeque").paramName("deque").paramType(parameterize(Deque.class, wildcardType().withUpperBounds(Object.class).build())).paramTypeArg(wildcardType().withUpperBounds(Object.class).build()).visibility(PUBLIC).declaringClass(ClassWithCollections.class).build(), //
                                CollectionSetter.builder().methodName("setSortedSetWild").paramName("sortedSetWild").paramType(parameterize(SortedSet.class, wildcardType().build())).paramTypeArg(wildcardType().build()).visibility(PUBLIC).declaringClass(ClassWithCollections.class).build(), //
                                ArraySetter.builder().methodName("setFloats").paramName("floats").paramType(float[].class).paramComponentType(float.class).visibility(PUBLIC).declaringClass(ClassWithCollections.class).build(), //
                                MapSetter.builder().methodName("setMap").paramName("map").paramType(parameterize(Map.class, String.class, Object.class)).keyType(String.class).valueType(Object.class).visibility(PUBLIC).declaringClass(ClassWithCollections.class).build(), //
                                MapSetter.builder().methodName("setMapTU").paramName("mapTU").paramType(parameterize(Map.class, typeVariableT(), typeVariableU())).keyType(typeVariableT()).valueType(typeVariableU()).visibility(PUBLIC).declaringClass(ClassWithCollections.class).build(), //
                                MapSetter.builder().methodName("setMapWildObj").paramName("mapWildObj").paramType(parameterize(Map.class, wildcardType().build(), Object.class)).keyType(wildcardType().build()).valueType(Object.class).visibility(PUBLIC).declaringClass(ClassWithCollections.class).build(), //
                                MapSetter.builder().methodName("setMapNoTypeArgs").paramName("mapNoTypeArgs").paramType(Map.class).keyType(Object.class).valueType(Object.class).visibility(PUBLIC).declaringClass(ClassWithCollections.class).build(), //
                                CollectionSetter.builder().methodName("setListWithTwoParams").paramName("listWithTwoParams").paramType(parameterize(ListWithTwoParams.class, String.class, Integer.class)).paramTypeArg(parameterize(Map.class, String.class, Integer.class)).visibility(PUBLIC).declaringClass(ClassWithCollections.class).build(), //
                                MapSetter.builder().methodName("setMapWithThreeParams").paramName("mapWithThreeParams").paramType(parameterize(MapWithThreeParams.class, String.class, Integer.class, Boolean.class)).keyType(String.class).valueType(parameterize(Map.class, Integer.class, Boolean.class)).visibility(PUBLIC).declaringClass(ClassWithCollections.class).build())), //
                Arguments.of("set", "get", false, PetJaxb.class, //
                        Set.of( //
                                SimpleSetter.builder().methodName("setFullName").paramName("fullName").paramType(String.class).visibility(PUBLIC).declaringClass(PetJaxb.class).build(), //
                                SimpleSetter.builder().methodName("setWeight").paramName("weight").paramType(float.class).visibility(PUBLIC).declaringClass(PetJaxb.class).build(), //
                                SimpleSetter.builder().methodName("setOwner").paramName("owner").paramType(PersonJaxb.class).visibility(PUBLIC).declaringClass(PetJaxb.class).build())),
                Arguments.of("set", "get", true, PetJaxb.class, //
                        Set.of( //
                                SimpleSetter.builder().methodName("setFullName").paramName("fullName").paramType(String.class).visibility(PUBLIC).declaringClass(PetJaxb.class).build(), //
                                SimpleSetter.builder().methodName("setWeight").paramName("weight").paramType(float.class).visibility(PUBLIC).declaringClass(PetJaxb.class).build(), //
                                CollectionGetAndAdder.builder().methodName("getSiblings").paramName("siblings").paramType(parameterize(List.class, PetJaxb.class)).paramTypeArg(PetJaxb.class).visibility(PUBLIC).declaringClass(PetJaxb.class).build(), //
                                SimpleSetter.builder().methodName("setOwner").paramName("owner").paramType(PersonJaxb.class).visibility(PUBLIC).declaringClass(PetJaxb.class).build())),
                Arguments.of("set", "myPrefix", true, PetJaxb.class, //
                        Set.of( //
                                SimpleSetter.builder().methodName("setFullName").paramName("fullName").paramType(String.class).visibility(PUBLIC).declaringClass(PetJaxb.class).build(), //
                                SimpleSetter.builder().methodName("setWeight").paramName("weight").paramType(float.class).visibility(PUBLIC).declaringClass(PetJaxb.class).build(), //
                                SimpleSetter.builder().methodName("setOwner").paramName("owner").paramType(PersonJaxb.class).visibility(PUBLIC).declaringClass(PetJaxb.class).build())), //
                Arguments.of("set", "get", true, GetAndAdd.class, //
                        Set.of( //
                                CollectionSetter.builder().methodName("setListGetterAndSetter").paramName("listGetterAndSetter").paramType(parameterize(List.class, String.class)).paramTypeArg(String.class).visibility(PUBLIC).declaringClass(GetAndAdd.class).build(), //
                                CollectionGetAndAdder.builder().methodName("getListNoSetter").paramName("listNoSetter").paramType(parameterize(List.class, String.class)).paramTypeArg(String.class).visibility(PUBLIC).declaringClass(GetAndAdd.class).build(), //
                                CollectionSetter.builder().methodName("setListNoGetter").paramName("listNoGetter").paramType(parameterize(List.class, String.class)).paramTypeArg(String.class).visibility(PUBLIC).declaringClass(GetAndAdd.class).build(), //
                                CollectionGetAndAdder.builder().methodName("getListSetterWrongType").paramName("listSetterWrongType").paramType(parameterize(List.class, String.class)).paramTypeArg(String.class).visibility(PUBLIC).declaringClass(GetAndAdd.class).build(), //
                                ArraySetter.builder().methodName("setListSetterWrongType").paramName("listSetterWrongType").paramType(String[].class).paramComponentType(String.class).visibility(PUBLIC).declaringClass(GetAndAdd.class).build())));
    }

    @ParameterizedTest
    @MethodSource
    void testGatherAllSettersForClassWithHierarchy(final Class<?> clazz, final Set<Setter> expected) {
        // Arrange
        when(properties.getSetterPrefix()).thenReturn("set");
        // Act
        final Set<Setter> actual = setterService.gatherAllSetters(clazz);
        // Assert
        assertThat(actual)
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withEqualsForFields(
                                (a, b) -> ((Type) a).getTypeName().equals(((Type) b).getTypeName()),
                                "paramType", "paramTypeArg", "keyType", "valueType")
                        .build())
                .isEqualTo(expected);
    }

    private static Stream<Arguments> testGatherAllSettersForClassWithHierarchy() {
        return Stream.of( //
                Arguments.of(
                        ClassWithHierarchy.class,
                        Set.of( //
                                SimpleSetter.builder().methodName("setOne").paramName("one").paramType(int.class).visibility(PUBLIC).declaringClass(ClassWithHierarchy.class).build(), //
                                SimpleSetter.builder().methodName("setTwo").paramName("two").paramType(int.class).visibility(PACKAGE_PRIVATE).declaringClass(FirstSuperClass.class).build(), //
                                SimpleSetter.builder().methodName("setThree").paramName("three").paramType(int.class).visibility(PUBLIC).declaringClass(AnInterface.class).build(), //
                                SimpleSetter.builder().methodName("setFour").paramName("four").paramType(int.class).visibility(PUBLIC).declaringClass(SecondSuperClassInDifferentPackage.class).build(), //
                                SimpleSetter.builder().methodName("setSeven").paramName("seven").paramType(int.class).visibility(PROTECTED).declaringClass(TopLevelSuperClass.class).build(), //
                                SimpleSetter.builder().methodName("setEight").paramName("eight").paramType(int.class).visibility(PUBLIC).declaringClass(AnotherInterface.class).build())), //
                Arguments.of(
                        GenericChild.class,
                        Set.of( //
                                CollectionSetter.builder().methodName("setList").paramName("list").paramType(parameterize(List.class, String.class)).paramTypeArg(String.class).visibility(PUBLIC).declaringClass(GenericChild.class).build(), //
                                MapSetter.builder().methodName("setMap").paramName("map").paramType(parameterize(Map.class, typeVariableS(), typeVariableT())).keyType(typeVariableS()).valueType(typeVariableT()).visibility(PUBLIC).declaringClass(GenericChild.class).build(), //
                                SimpleSetter.builder().methodName("setGeneric").paramName("generic").paramType(parameterize(Generic.class, typeVariableT())).visibility(PUBLIC).declaringClass(GenericChild.class).build(), //
                                SimpleSetter.builder().methodName("setOtherGeneric").paramName("otherGeneric").paramType(parameterize(Generic.class, String.class)).visibility(PUBLIC).declaringClass(GenericParent.class).build())), //
                Arguments.of(
                        GenericGrandChild.class,
                        Set.of( //
                                CollectionSetter.builder().methodName("setList").paramName("list").paramType(parameterize(List.class, String.class)).paramTypeArg(String.class).visibility(PUBLIC).declaringClass(GenericChild.class).build(), //
                                MapSetter.builder().methodName("setMap").paramName("map").paramType(parameterize(Map.class, Long.class, Boolean.class)).keyType(Long.class).valueType(Boolean.class).visibility(PUBLIC).declaringClass(GenericGrandChild.class).build(), //
                                SimpleSetter.builder().methodName("setGeneric").paramName("generic").paramType(parameterize(Generic.class, Boolean.class)).visibility(PUBLIC).declaringClass(GenericGrandChild.class).build(),
                                SimpleSetter.builder().methodName("setOtherGeneric").paramName("otherGeneric").paramType(parameterize(Generic.class, String.class)).visibility(PUBLIC).declaringClass(GenericParent.class).build()))); //
    }

    private static TypeVariable<?> typeVariableS() {
        return GenericParent.class.getTypeParameters()[1];
    }

    private static TypeVariable<?> typeVariableT() {
        return ClassWithCollections.class.getTypeParameters()[0];
    }

    private static TypeVariable<?> typeVariableU() {
        return ClassWithCollections.class.getTypeParameters()[1];
    }
}