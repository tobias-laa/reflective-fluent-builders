package io.github.tobi.laa.reflective.fluent.builders.service.impl;

import com.google.common.collect.ImmutableSet;
import io.github.tobi.laa.reflective.fluent.builders.service.api.TypeService;
import lombok.RequiredArgsConstructor;

import javax.inject.Named;
import javax.inject.Singleton;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * Standard implementation of {@link TypeService}.
 * </p>
 */
@Named
@Singleton
class TypeServiceImpl implements TypeService {

    @Override
    public Set<Class<?>> explodeType(final Type type) {
        Objects.requireNonNull(type);
        return new TypeCollector(type).collect();
    }

    @RequiredArgsConstructor
    private static class TypeCollector {

        private final ImmutableSet.Builder<Class<?>> builder = ImmutableSet.builder();

        private final Set<Type> alreadyProcessedTypes = new HashSet<>();

        private final Type type;

        Set<Class<?>> collect() {
            collect(type);
            return builder.build();
        }

        @SuppressWarnings("rawtypes")
        private void collect(final Type type) {
            if (!alreadyProcessedTypes.contains(type)) {
                alreadyProcessedTypes.add(type);
                if (type instanceof GenericArrayType genericArrayType) {
                    collect(genericArrayType.getGenericComponentType());
                } else if (type instanceof ParameterizedType parameterizedType) {
                    collect(parameterizedType.getRawType());
                    Arrays.stream(parameterizedType.getActualTypeArguments()).forEach(this::collect);
                } else if (type instanceof TypeVariable typeVariable) {
                    Arrays.stream(typeVariable.getBounds()).forEach(this::collect);
                } else if (type instanceof WildcardType wildcardType) {
                    Arrays.stream(wildcardType.getUpperBounds()).forEach(this::collect);
                    Arrays.stream(wildcardType.getLowerBounds()).forEach(this::collect);
                } else {
                    builder.add((Class<?>) type);
                }
            }
        }
    }
}
