package io.github.tobi.laa.reflective.fluent.builders.generator.impl;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import io.github.tobi.laa.reflective.fluent.builders.generator.api.MethodCodeGenerator;
import io.github.tobi.laa.reflective.fluent.builders.model.BuilderMetadata;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.lang.model.element.Modifier;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static io.github.tobi.laa.reflective.fluent.builders.constants.BuilderConstants.OBJECT_SUPPLIER_FIELD_NAME;

/**
 * <p>
 * Implementation of {@link MethodCodeGenerator} for generating a constructor that takes a
 * {@link java.util.function.Supplier} for initially constructing the object to be modified.
 * </p>
 */
@Named
@Singleton
class ConstructorWithObjectSupplierCodeGenerator implements MethodCodeGenerator {

    @Override
    public Optional<MethodSpec> generate(final BuilderMetadata builderMetadata) {
        Objects.requireNonNull(builderMetadata);
        final var builtType = builderMetadata.getBuiltType().getType().loadClass();
        final var supplierTypeName = ParameterizedTypeName.get(Supplier.class, builtType);
        return Optional.of(MethodSpec.constructorBuilder()
                .addJavadoc("Creates a new instance of {@link $T} using the given {@code $L}.\n", builtType, OBJECT_SUPPLIER_FIELD_NAME)
                .addJavadoc("Has been set to visibility {@code protected} so that users may choose to inherit the builder.\n")
                .addModifiers(Modifier.PROTECTED)
                .addParameter(supplierTypeName, OBJECT_SUPPLIER_FIELD_NAME, Modifier.FINAL)
                .addStatement("this.$1L = $2T.requireNonNull($1L)", OBJECT_SUPPLIER_FIELD_NAME, Objects.class)
                .build());
    }
}
