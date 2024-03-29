package io.github.tobi.laa.reflective.fluent.builders.model;

import java.lang.reflect.ParameterizedType;
import java.util.Comparator;

/**
 * <p>
 * Compares {@link PropertyType types} in the context of being a method parameter - that is types with generics are
 * considered equal even if their type arguments do not match.
 * </p>
 */
class ParamTypeComparator implements Comparator<PropertyType> {

    @Override
    public int compare(final PropertyType a, final PropertyType b) {
        if (a == null) {
            return -1;
        } else if (b == null) {
            return 1;
        } else if (a.getType() instanceof ParameterizedType typeA && b.getType() instanceof ParameterizedType typeB) {
            final var aRawType = typeA.getRawType();
            final var bRawType = typeB.getRawType();
            return aRawType.getTypeName().compareTo(bRawType.getTypeName());
        } else {
            return a.getType().getTypeName().compareTo(b.getType().getTypeName());
        }
    }
}
