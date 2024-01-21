package io.github.tobi.laa.reflective.fluent.builders.model;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.Objects;

import static java.util.Objects.compare;

/**
 * <p>
 * Common logic shared by {@link WriteAccessor} implementations that represent a method, i.e. setters, adders and
 * getters of collections and maps.
 * </p>
 */
@SuperBuilder(toBuilder = true)
@Data
abstract class AbstractMethodAccessor extends AbstractWriteAccessor {

    /**
     * <p>
     * The name of the method, for instance {@code setAge}.
     * </p>
     */
    @lombok.NonNull
    private final String methodName;

    @Override
    public boolean equals(final Object anObject) {
        if (this == anObject) {
            return true;
        } else if (anObject == null || anObject.getClass() != this.getClass()) {
            return false;
        }
        final var aSetter = (AbstractMethodAccessor) anObject;
        return Objects.equals(getMethodName(), aSetter.getMethodName()) && //
                compare(getPropertyType(), aSetter.getPropertyType(), new ParamTypeComparator()) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMethodName(), getPropertyType());
    }
}