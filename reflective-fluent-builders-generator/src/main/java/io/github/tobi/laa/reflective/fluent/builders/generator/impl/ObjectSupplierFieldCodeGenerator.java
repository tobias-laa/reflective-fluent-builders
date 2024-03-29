package io.github.tobi.laa.reflective.fluent.builders.generator.impl;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import io.github.tobi.laa.reflective.fluent.builders.generator.api.FieldCodeGenerator;
import io.github.tobi.laa.reflective.fluent.builders.model.BuilderMetadata;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Objects;
import java.util.function.Supplier;

import static io.github.tobi.laa.reflective.fluent.builders.constants.BuilderConstants.OBJECT_SUPPLIER_FIELD_NAME;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;

/**
 * <p>
 * Generates the field
 * {@link io.github.tobi.laa.reflective.fluent.builders.constants.BuilderConstants#OBJECT_SUPPLIER_FIELD_NAME objectSupplier}.
 * </p>
 */
@Singleton
@Named
class ObjectSupplierFieldCodeGenerator implements FieldCodeGenerator {

    @Override
    public FieldSpec generate(final BuilderMetadata builderMetadata) {
        Objects.requireNonNull(builderMetadata);
        final var supplierTypeName = ParameterizedTypeName.get(Supplier.class, builderMetadata.getBuiltType().getType().loadClass());
        return FieldSpec.builder(supplierTypeName, OBJECT_SUPPLIER_FIELD_NAME, PRIVATE, FINAL).build();
    }
}
