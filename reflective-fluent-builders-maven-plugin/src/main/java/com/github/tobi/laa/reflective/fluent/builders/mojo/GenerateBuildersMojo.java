package com.github.tobi.laa.reflective.fluent.builders.mojo;

import com.github.tobi.laa.reflective.fluent.builders.constants.BuilderConstants;
import com.github.tobi.laa.reflective.fluent.builders.generator.api.JavaFileGenerator;
import com.github.tobi.laa.reflective.fluent.builders.model.BuilderMetadata;
import com.github.tobi.laa.reflective.fluent.builders.props.impl.StandardBuildersProperties;
import com.github.tobi.laa.reflective.fluent.builders.service.api.BuilderMetadataService;
import com.github.tobi.laa.reflective.fluent.builders.service.api.ClassService;
import com.google.common.collect.Sets;
import com.squareup.javapoet.JavaFile;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Mojo(name = "generate-builders", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class GenerateBuildersMojo extends AbstractMojo {

    @Setter(onMethod_ =
    @Parameter(property = "builderPackage", defaultValue = BuilderConstants.PACKAGE_PLACEHOLDER))
    private String builderPackage;

    @Setter(onMethod_ =
    @Parameter(property = "builderSuffix", defaultValue = "Builder"))
    private String builderSuffix;

    @Setter(onMethod_ =
    @Parameter(property = "setterPrefix", defaultValue = "set"))
    private String setterPrefix;

    @Setter(onMethod_ =
    @Parameter(property = "hierarchyCollection.classesToExclude"))
    private Set<Class<?>> classesToExclude = Collections.singleton(Object.class);

    @Setter(onMethod_ =
    @Parameter(required = true, property = "packageToScan"))
    private String packageToScan;

    @Setter(onMethod_ =
    @Parameter(property = "target"))
    private File target;

    @Setter(onMethod_ =
    @Parameter(property = "addCompileSourceRoot", defaultValue = "true"))
    private boolean addCompileSourceRoot;

    @Setter(onMethod_ =
    @Parameter(readonly = true, required = true, defaultValue = "${project}"))
    private MavenProject mavenProject;

    @Setter(onMethod_ =
    @Parameter(readonly = true, required = true, defaultValue = "${mojoExecution}"))
    private MojoExecution mojoExecution;

    @lombok.NonNull
    private final StandardBuildersProperties buildersProperties;

    @lombok.NonNull
    private final JavaFileGenerator javaFileGenerator;

    @lombok.NonNull
    private final ClassService classService;

    @lombok.NonNull
    private final BuilderMetadataService builderMetadataService;

    @Override
    public void execute() throws MojoFailureException {
        mapMavenParamsToProps();
        final Set<Class<?>> buildableClasses = collectBuildableClasses();
        setDefaultTargetDirectoryIfNecessary();
        createTargetDirectory();
        final Set<BuilderMetadata> nonEmptyBuilderMetadata = collectNonEmptyBuilderMetadata(buildableClasses);
        generateAndWriteBuildersToTarget(nonEmptyBuilderMetadata);
        addCompileSourceRoot();
    }

    private void mapMavenParamsToProps() {
        buildersProperties.setBuilderPackage(builderPackage);
        buildersProperties.setBuilderSuffix(builderSuffix);
        buildersProperties.setSetterPrefix(setterPrefix);
        buildersProperties.getHierarchyCollection().setClassesToExclude(classesToExclude);
        getLog().debug("Properties are: " + buildersProperties);
    }

    private Set<Class<?>> collectBuildableClasses() {
        getLog().info("Scan package " + packageToScan + " recursively for classes.");
        final Set<Class<?>> allClasses = classService.collectClassesRecursively(packageToScan.trim());
        final Set<Class<?>> buildableClasses = builderMetadataService.filterOutNonBuildableClasses(allClasses);
        if (getLog().isDebugEnabled()) {
            getLog().debug("The following classes can be built:");
            buildableClasses.forEach(c -> getLog().debug("- " + c.getName()));
            final Set<Class<?>> nonBuildableClasses = Sets.difference(allClasses, buildableClasses);
            getLog().debug("The following classes cannot be built:");
            nonBuildableClasses.forEach(c -> getLog().debug("- " + c.getName()));
        }
        getLog().info("Found " + buildableClasses.size() + " buildable classes.");
        return buildableClasses;
    }

    private void setDefaultTargetDirectoryIfNecessary() {
        if (target == null && isTestPhase()) {
            target = Paths.get(mavenProject.getBuild().getDirectory()) //
                    .resolve("generated-test-sources") //
                    .resolve("builders") //
                    .toFile();
        } else if (target == null) {
            target = Paths.get(mavenProject.getBuild().getDirectory())
                    .resolve("generated-sources") //
                    .resolve("builders") //
                    .toFile();
        }
    }

    private void createTargetDirectory() throws MojoFailureException {
        getLog().info("Make sure target directory " + target + " exists.");
        try {
            Files.createDirectories(target.toPath());
        } catch (final IOException e) {
            throw new MojoFailureException("Could not create target directory " + target + '.', e);
        }
    }

    private Set<BuilderMetadata> collectNonEmptyBuilderMetadata(final Set<Class<?>> buildableClasses) {
        final Set<BuilderMetadata> allMetadata = buildableClasses.stream() //
                .map(builderMetadataService::collectBuilderMetadata) //
                .collect(Collectors.toSet());
        final Set<BuilderMetadata> nonEmptyMetadata = builderMetadataService.filterOutEmptyBuilders(allMetadata);
        if (getLog().isDebugEnabled()) {
            final Set<BuilderMetadata> emptyMetadata = Sets.difference(allMetadata, nonEmptyMetadata);
            getLog().debug("Builders for the following classes would be empty and will thus be skipped:");
            emptyMetadata.forEach(m -> getLog().debug("- " + m.getBuiltType().getType().getName()));
        }
        return nonEmptyMetadata;
    }

    private void generateAndWriteBuildersToTarget(Set<BuilderMetadata> nonEmptyBuilderMetadata) throws MojoFailureException {
        for (final BuilderMetadata metadata : nonEmptyBuilderMetadata) {
            getLog().info("Generate builder for class " + metadata.getBuiltType().getType().getName());
            final JavaFile javaFile = javaFileGenerator.generateJavaFile(metadata);
            try {
                javaFile.writeTo(target);
            } catch (final IOException e) {
                throw new MojoFailureException("Could not create file for builder for " + metadata.getBuiltType().getType().getName() + '.', e);
            }
        }
    }

    private void addCompileSourceRoot() {
        if (addCompileSourceRoot) {
            final String path = target.getPath();
            if (isTestPhase()) {
                getLog().debug("Add " + path + " as test source folder.");
                mavenProject.addTestCompileSourceRoot(target.getPath());
            } else {
                getLog().debug("Add " + path + " as source folder.");
                mavenProject.addCompileSourceRoot(target.getPath());
            }
        }
    }

    private boolean isTestPhase() {
        return StringUtils.containsIgnoreCase(mojoExecution.getLifecyclePhase(), "test");
    }
}
