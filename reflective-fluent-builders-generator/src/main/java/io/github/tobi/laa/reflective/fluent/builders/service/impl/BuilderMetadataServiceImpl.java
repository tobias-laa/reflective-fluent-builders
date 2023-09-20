package io.github.tobi.laa.reflective.fluent.builders.service.impl;

import io.github.tobi.laa.reflective.fluent.builders.model.BuilderMetadata;
import io.github.tobi.laa.reflective.fluent.builders.model.Setter;
import io.github.tobi.laa.reflective.fluent.builders.props.api.BuildersProperties;
import io.github.tobi.laa.reflective.fluent.builders.service.api.*;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static io.github.tobi.laa.reflective.fluent.builders.constants.BuilderConstants.GENERATED_BUILDER_MARKER_FIELD_NAME;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.function.Predicate.not;

/**
 * <p>
 * Standard implementation of {@link BuilderMetadataService}.
 * </p>
 */
@Named
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
class BuilderMetadataServiceImpl implements BuilderMetadataService {

    @lombok.NonNull
    private final AccessibilityService accessibilityService;

    @lombok.NonNull
    private final SetterService setterService;

    @lombok.NonNull
    private final ClassService classService;

    @lombok.NonNull
    private final BuilderPackageService builderPackageService;

    @lombok.NonNull
    private final BuildersProperties properties;

    @Override
    public BuilderMetadata collectBuilderMetadata(final Class<?> clazz) {
        Objects.requireNonNull(clazz);
        final String builderPackage = builderPackageService.resolveBuilderPackage(clazz);
        return BuilderMetadata.builder() //
                .packageName(builderPackage) //
                .name(builderClassName(clazz, builderPackage)) //
                .builtType(BuilderMetadata.BuiltType.builder() //
                        .type(clazz) //
                        .location(classService.determineClassLocation(clazz).orElse(null)) //
                        .accessibleNonArgsConstructor(hasAccessibleNonArgsConstructor(clazz, builderPackage)) //
                        .setters(gatherSettersAndAvoidNameCollisions(clazz))
                        .build()) //
                .build();
    }

    private String builderClassName(final Class<?> clazz, final String builderPackage) {
        var name = clazz.getSimpleName() + properties.getBuilderSuffix();
        int count = 0;
        while (builderAlreadyExists(builderPackage + '.' + name)) {
            name = clazz.getSimpleName() + properties.getBuilderSuffix() + count;
            count++;
        }
        return name;
    }

    private boolean builderAlreadyExists(final String builderClassName) {
        final Optional<Class<?>> builderClass = classService.loadClass(builderClassName);
        if (builderClass.isEmpty()) {
            return false;
        } else {
            return builderClass
                    .stream() //
                    .map(Class::getDeclaredFields) //
                    .flatMap(Arrays::stream) //
                    .map(Field::getName) //
                    .noneMatch(GENERATED_BUILDER_MARKER_FIELD_NAME::equals);
        }
    }

    private boolean hasAccessibleNonArgsConstructor(final Class<?> clazz, final String builderPackage) {
        return Arrays //
                .stream(clazz.getDeclaredConstructors()) //
                .filter(constructor -> accessibilityService.isAccessibleFrom(constructor, builderPackage)) //
                .mapToInt(Constructor::getParameterCount) //
                .anyMatch(count -> count == 0);
    }

    private SortedSet<Setter> gatherSettersAndAvoidNameCollisions(final Class<?> clazz) {
        final var setters = setterService.gatherAllSetters(clazz);
        return avoidNameCollisions(setters);
    }

    private SortedSet<Setter> avoidNameCollisions(final Set<Setter> setters) {
        final SortedSet<Setter> noNameCollisions = new TreeSet<>();
        for (final var setter : setters) {
            if (noNameCollisions.stream().map(Setter::getParamName).noneMatch(setter.getParamName()::equals)) {
                noNameCollisions.add(setter);
            } else {
                for (int i = 0; true; i++) {
                    final var paramName = setter.getParamName() + i;
                    if (noNameCollisions.stream().map(Setter::getParamName).noneMatch(paramName::equals)) {
                        noNameCollisions.add(setter.withParamName(paramName));
                        break;
                    }
                }
            }
        }
        return noNameCollisions;
    }

    @Override
    public Set<Class<?>> filterOutNonBuildableClasses(final Set<Class<?>> classes) {
        Objects.requireNonNull(classes);
        return classes //
                .stream() //
                .filter(not(Class::isInterface)) //
                .filter(not(Class::isAnonymousClass)) //
                .filter(not(Class::isEnum)) //
                .filter(not(Class::isPrimitive)) //
                .filter(not(classService::isAbstract)) //
                .filter(not(clazz -> clazz.isMemberClass() && !isStatic(clazz.getModifiers()))) //
                .filter(clazz -> accessibilityService.isAccessibleFrom(clazz, builderPackageService.resolveBuilderPackage(clazz))) //
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Class<?>> filterOutConfiguredExcludes(final Set<Class<?>> classes) {
        Objects.requireNonNull(classes);
        return classes.stream().filter(not(this::exclude)).collect(Collectors.toSet());
    }

    private boolean exclude(final Class<?> clazz) {
        return properties.getExcludes().stream().anyMatch(p -> p.test(clazz));
    }

    @Override
    public Set<BuilderMetadata> filterOutEmptyBuilders(final Collection<BuilderMetadata> builderMetadata) {
        Objects.requireNonNull(builderMetadata);
        return builderMetadata.stream() //
                .filter(metadata -> !metadata.getBuiltType().getSetters().isEmpty()) //
                .collect(Collectors.toSet());
    }
}
