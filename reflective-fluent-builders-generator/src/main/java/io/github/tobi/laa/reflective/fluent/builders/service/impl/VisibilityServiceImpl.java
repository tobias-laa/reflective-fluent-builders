package io.github.tobi.laa.reflective.fluent.builders.service.impl;

import io.github.tobi.laa.reflective.fluent.builders.model.Visibility;
import io.github.tobi.laa.reflective.fluent.builders.service.api.VisibilityService;

import javax.inject.Named;
import javax.inject.Singleton;
import java.lang.reflect.Modifier;

/**
 * <p>
 * Standard implementation of {@link VisibilityService}.
 * </p>
 */
@Named
@Singleton
class VisibilityServiceImpl implements VisibilityService {

    @Override
    public Visibility toVisibility(final int modifiers) {
        if (Modifier.isPrivate(modifiers)) {
            return Visibility.PRIVATE;
        } else if (Modifier.isProtected(modifiers)) {
            return Visibility.PROTECTED;
        } else if (Modifier.isPublic(modifiers)) {
            return Visibility.PUBLIC;
        } else {
            return Visibility.PACKAGE_PRIVATE;
        }
    }
}
