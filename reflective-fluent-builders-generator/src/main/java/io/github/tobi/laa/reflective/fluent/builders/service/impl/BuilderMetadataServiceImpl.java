package io.github.tobi.laa.reflective.fluent.builders.service.impl;

import com.google.common.collect.ImmutableSortedSet;
import io.github.tobi.laa.reflective.fluent.builders.model.BuilderMetadata;
import io.github.tobi.laa.reflective.fluent.builders.model.Setter;
import io.github.tobi.laa.reflective.fluent.builders.model.Visibility;
import io.github.tobi.laa.reflective.fluent.builders.props.api.BuildersProperties;
import io.github.tobi.laa.reflective.fluent.builders.service.api.BuilderMetadataService;
import io.github.tobi.laa.reflective.fluent.builders.service.api.ClassService;
import io.github.tobi.laa.reflective.fluent.builders.service.api.SetterService;
import io.github.tobi.laa.reflective.fluent.builders.service.api.VisibilityService;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import static io.github.tobi.laa.reflective.fluent.builders.constants.BuilderConstants.PACKAGE_PLACEHOLDER;
import static io.github.tobi.laa.reflective.fluent.builders.model.Visibility.*;
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
    private final VisibilityService visibilityService;

    @lombok.NonNull
    private final SetterService setterService;

    @lombok.NonNull
    private final ClassService classService;

    @lombok.NonNull
    private final BuildersProperties properties;

    @Override
    public BuilderMetadata collectBuilderMetadata(final Class<?> clazz) {
        Objects.requireNonNull(clazz);
        final String builderPackage = resolveBuilderPackage(clazz);
        return BuilderMetadata.builder() //
                .packageName(builderPackage) //
                .name(clazz.getSimpleName() + properties.getBuilderSuffix()) //
                .builtType(BuilderMetadata.BuiltType.builder() //
                        .type(clazz) //
                        .location(classService.determineClassLocation(clazz).orElse(null)) //
                        .accessibleNonArgsConstructor(hasAccessibleNonArgsConstructor(clazz, builderPackage)) //
                        .setters(gatherAndFilterAccessibleSettersAndAvoidNameCollisions(clazz, builderPackage))
                        .build()) //
                .build();
    }

    private String resolveBuilderPackage(final Class<?> clazz) {
        return properties.getBuilderPackage().replace(PACKAGE_PLACEHOLDER, clazz.getPackageName());
    }

    private boolean hasAccessibleNonArgsConstructor(final Class<?> clazz, final String builderPackage) {
        return Arrays //
                .stream(clazz.getConstructors()) //
                .filter(constructor -> isAccessible(constructor, builderPackage)) //
                .mapToInt(Constructor::getParameterCount) //
                .anyMatch(count -> count == 0);
    }

    private boolean isAccessible(final Constructor<?> constructor, final String builderPackage) {
        return isAccessible(constructor.getDeclaringClass(), constructor.getModifiers(), builderPackage);
    }

    private SortedSet<Setter> gatherAndFilterAccessibleSettersAndAvoidNameCollisions(final Class<?> clazz, final String builderPackage) {
        final var setters = setterService.gatherAllSetters(clazz) //
                .stream() //
                .filter(setter -> isAccessibleSetter(setter, builderPackage)) //
                .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder()));
        return avoidNameCollisions(setters);
    }

    private boolean isAccessibleSetter(final Setter setter, final String builderPackage) {
        return isAccessible(setter.getDeclaringClass(), setter.getVisibility(), builderPackage) && //
                isAccessibleParamType(setter.getParamType(), builderPackage);
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
                .filter(not(this::isAbstract)) //
                .filter(not(Class::isAnonymousClass)) //
                .filter(not(Class::isEnum)) //
                .filter(not(Class::isPrimitive)) //
                .filter(not(clazz -> clazz.isMemberClass() && !isStatic(clazz.getModifiers()))) //
                .filter(clazz -> isAccessible(clazz, resolveBuilderPackage(clazz))) //
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

    private boolean isAbstract(final Class<?> clazz) {
        return Modifier.isAbstract(clazz.getModifiers());
    }

    private boolean isAccessible(final Class<?> clazz, final String builderPackage) {
        return isAccessible(clazz, clazz.getModifiers(), builderPackage);
    }

    private boolean isAccessibleParamType(final Type paramType, final String builderPackage) {
        final var clazz = (Class<?>) paramType;
        final var visibility = visibilityService.toVisibility(clazz.getModifiers());
        return isAccessible(clazz, visibility, builderPackage);
    }

    private boolean isAccessible(final Class<?> clazz, final int modifiers, final String builderPackage) {
        final var visibility = visibilityService.toVisibility(modifiers);
        return isAccessible(clazz, visibility, builderPackage);
    }

    private boolean isAccessible(final Class<?> clazz, final Visibility visibility, final String builderPackage) {
        return visibility == PUBLIC || //
                visibility == PACKAGE_PRIVATE && !isAbstract(clazz) && builderPackage.equals(clazz.getPackage().getName()) || //
                visibility == PROTECTED && builderPackage.equals(clazz.getPackage().getName());
    }

    @Override
    public Set<BuilderMetadata> filterOutEmptyBuilders(final Collection<BuilderMetadata> builderMetadata) {
        Objects.requireNonNull(builderMetadata);
        return builderMetadata.stream() //
                .filter(metadata -> !metadata.getBuiltType().getSetters().isEmpty()) //
                .collect(Collectors.toSet());
    }
}
