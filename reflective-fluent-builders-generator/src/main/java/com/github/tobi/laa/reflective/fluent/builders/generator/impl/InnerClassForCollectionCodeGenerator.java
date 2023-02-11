package com.github.tobi.laa.reflective.fluent.builders.generator.impl;

import com.github.tobi.laa.reflective.fluent.builders.constants.BuilderConstants.CallSetterFor;
import com.github.tobi.laa.reflective.fluent.builders.constants.BuilderConstants.FieldValue;
import com.github.tobi.laa.reflective.fluent.builders.exception.CodeGenerationException;
import com.github.tobi.laa.reflective.fluent.builders.generator.api.BuilderClassNameGenerator;
import com.github.tobi.laa.reflective.fluent.builders.generator.api.CollectionClassCodeGenerator;
import com.github.tobi.laa.reflective.fluent.builders.generator.api.CollectionInitializerCodeGenerator;
import com.github.tobi.laa.reflective.fluent.builders.generator.model.CollectionClassSpec;
import com.github.tobi.laa.reflective.fluent.builders.model.BuilderMetadata;
import com.github.tobi.laa.reflective.fluent.builders.model.CollectionSetter;
import com.github.tobi.laa.reflective.fluent.builders.model.Setter;
import com.github.tobi.laa.reflective.fluent.builders.service.api.SetterService;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import lombok.RequiredArgsConstructor;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Objects;

import static javax.lang.model.element.Modifier.FINAL;
import static org.apache.commons.lang3.StringUtils.capitalize;

/**
 * <p>
 * Implementation of {@link CollectionClassCodeGenerator} for generating inner classes for convenient collection
 * construction.
 * </p>
 */
@RequiredArgsConstructor
public class InnerClassForCollectionCodeGenerator implements CollectionClassCodeGenerator {

    @lombok.NonNull
    private final BuilderClassNameGenerator builderClassNameGenerator;

    @lombok.NonNull
    private final SetterService setterService;

    @lombok.NonNull
    private final List<CollectionInitializerCodeGenerator> initializerGenerators;

    @Override
    public boolean isApplicable(final Setter setter) {
        Objects.requireNonNull(setter);
        return setter instanceof final CollectionSetter collectionSetter //
                && initializerGenerators.stream().anyMatch(gen -> gen.isApplicable(collectionSetter));
    }

    @Override
    public CollectionClassSpec generate(final BuilderMetadata builderMetadata, final Setter setter) {
        Objects.requireNonNull(builderMetadata);
        Objects.requireNonNull(setter);
        if (setter instanceof final CollectionSetter collectionSetter) {
            return generate(builderMetadata, collectionSetter);
        } else {
            throw new CodeGenerationException("Generation of inner collection class for " + setter + " is not supported.");
        }
    }

    private CollectionClassSpec generate(final BuilderMetadata builderMetadata, final CollectionSetter setter) {
        final var builderClassName = builderClassNameGenerator.generateClassName(builderMetadata);
        final var className = builderClassName.nestedClass("Collection" + capitalize(setter.getParamName()));
        return CollectionClassSpec.builder() //
                .getter(MethodSpec //
                        .methodBuilder(setterService.dropSetterPrefix(setter.getMethodName())) //
                        .addModifiers(Modifier.PUBLIC) //
                        .returns(className) //
                        .addStatement("return new $T()", className) //
                        .build()) //
                .innerClass(TypeSpec //
                        .classBuilder(className) //
                        .addModifiers(Modifier.PUBLIC) //
                        .addMethod(MethodSpec.methodBuilder("add") //
                                .addModifiers(Modifier.PUBLIC) //
                                .addParameter(setter.getParamTypeArg(), "item", FINAL) //
                                .returns(className) //
                                .beginControlFlow("if ($T.this.$L.$L == null)", builderClassName, FieldValue.FIELD_NAME, setter.getParamName()) //
                                .addStatement(CodeBlock.builder()
                                        .add("$T.this.$L.$L = ", builderClassName, FieldValue.FIELD_NAME, setter.getParamName())
                                        .add(initializerGenerators //
                                                .stream() //
                                                .filter(gen -> gen.isApplicable(setter)) //
                                                .map(gen -> gen.generateCollectionInitializer(setter)) //
                                                .findFirst() //
                                                .orElseThrow(() -> new CodeGenerationException("Could not generate initializer for " + setter + '.'))) //
                                        .build()) //
                                .endControlFlow() //
                                .addStatement("$T.this.$L.$L.add($L)", builderClassName, FieldValue.FIELD_NAME, setter.getParamName(), "item") //
                                .addStatement("$T.this.$L.$L = $L", builderClassName, CallSetterFor.FIELD_NAME, setter.getParamName(), true) //
                                .addStatement("return this") //
                                .build()) //
                        .addMethod(MethodSpec.methodBuilder("and") //
                                .addModifiers(Modifier.PUBLIC) //
                                .returns(builderClassName) //
                                .addStatement("return $T.this", builderClassName) //
                                .build()) //
                        .build()) //
                .build();
    }
}