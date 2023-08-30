package io.github.tobi.laa.reflective.fluent.builders.test.models.nested;

import java.util.Objects;
import javax.annotation.processing.Generated;

@Generated(
    value = "io.github.tobi.laa.reflective.fluent.builders.generator.api.JavaFileGenerator",
    date = "3333-03-13T00:00Z[UTC]"
)
public class NestedProtectedLevelOneBuilder {
  /**
   * This field is solely used to be able to detect generated builders via reflection at a later stage.
   */
  private boolean ______generatedByReflectiveFluentBuildersGenerator;

  private TopLevelClass.NestedProtectedLevelOne objectToBuild;

  private final CallSetterFor callSetterFor = new CallSetterFor();

  private final FieldValue fieldValue = new FieldValue();

  protected NestedProtectedLevelOneBuilder(
      final TopLevelClass.NestedProtectedLevelOne objectToBuild) {
    this.objectToBuild = objectToBuild;
  }

  protected NestedProtectedLevelOneBuilder() {
    // noop
  }

  public static NestedProtectedLevelOneBuilder newInstance() {
    return new NestedProtectedLevelOneBuilder();
  }

  public static NestedProtectedLevelOneBuilder thatModifies(
      final TopLevelClass.NestedProtectedLevelOne objectToModify) {
    Objects.requireNonNull(objectToModify);
    return new NestedProtectedLevelOneBuilder(objectToModify);
  }

  public NestedProtectedLevelOneBuilder field(final int field) {
    this.fieldValue.field = field;
    this.callSetterFor.field = true;
    return this;
  }

  public TopLevelClass.NestedProtectedLevelOne build() {
    if (this.objectToBuild == null) {
      this.objectToBuild = new TopLevelClass.NestedProtectedLevelOne();
    }
    if (this.callSetterFor.field) {
      this.objectToBuild.setField(this.fieldValue.field);
    }
    return this.objectToBuild;
  }

  private class CallSetterFor {
    boolean field;
  }

  private class FieldValue {
    int field;
  }
}
