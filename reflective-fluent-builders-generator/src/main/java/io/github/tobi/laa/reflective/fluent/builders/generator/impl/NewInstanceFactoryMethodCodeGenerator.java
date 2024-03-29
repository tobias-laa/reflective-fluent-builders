package io.github.tobi.laa.reflective.fluent.builders.generator.impl;

import com.squareup.javapoet.MethodSpec;
import io.github.tobi.laa.reflective.fluent.builders.generator.api.BuilderClassNameGenerator;
import io.github.tobi.laa.reflective.fluent.builders.generator.api.MethodCodeGenerator;
import io.github.tobi.laa.reflective.fluent.builders.model.BuilderMetadata;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.lang.model.element.Modifier;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Generates a {@code newInstance} factory method for a builder in cases where the object to be built has an
 * accessible no-args constructor.
 * </p>
 */
@Named
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
class NewInstanceFactoryMethodCodeGenerator implements MethodCodeGenerator {

    @lombok.NonNull
    private final BuilderClassNameGenerator builderClassNameGenerator;

    @Override
    public Optional<MethodSpec> generate(final BuilderMetadata builderMetadata) {
        Objects.requireNonNull(builderMetadata);
        final var builderClassName = builderClassNameGenerator.generateClassName(builderMetadata);
        if (builderMetadata.getBuiltType().isAccessibleNonArgsConstructor()) {
            final var builtType = builderMetadata.getBuiltType().getType().loadClass();
            return Optional.of(MethodSpec.methodBuilder("newInstance")
                    .addJavadoc(
                            "Creates an instance of {@link $T} that will work on a new instance of {@link $T} once {@link #build()} is called.\n",
                            builderClassName,
                            builtType)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(builderClassName)
                    .addStatement("return new $T($T::new)", builderClassName, builtType)
                    .build());
        } else {
            return Optional.empty();
        }
    }
}
