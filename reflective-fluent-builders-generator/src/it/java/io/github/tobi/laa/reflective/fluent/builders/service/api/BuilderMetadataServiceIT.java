package io.github.tobi.laa.reflective.fluent.builders.service.api;

import io.github.tobi.laa.reflective.fluent.builders.sisu.SisuTest;
import io.github.tobi.laa.reflective.fluent.builders.test.models.complex.ClassWithBuilderExisting;
import io.github.tobi.laa.reflective.fluent.builders.test.models.simple.SimpleClass;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SisuTest
@RequiredArgsConstructor(onConstructor_ = @Inject)
class BuilderMetadataServiceIT {

    @lombok.NonNull
    private final BuilderMetadataService builderMetadataService;

    @Test
    void testFilterOutConfiguredExcludesWithDefaultConfig() {
        // Act
        final var filteredClasses = builderMetadataService.filterOutConfiguredExcludes(Set.of( //
                SimpleClass.class, //
                ClassWithBuilderExisting.class, //
                ClassWithBuilderExisting.ClassWithBuilderExistingBuilder.class, //
                HasTheSuffixBuilderImpl.class));
        // Assert
        assertThat(filteredClasses).contains(SimpleClass.class, ClassWithBuilderExisting.class);
    }

    static class HasTheSuffixBuilderImpl {
        // no content
    }
}
