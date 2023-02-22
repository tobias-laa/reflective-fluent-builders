package com.github.tobi.laa.reflective.fluent.builders.test.models.complex;

import java.lang.Float;
import java.lang.Object;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.processing.Generated;

@Generated(
    value = "com.github.tobi.laa.reflective.fluent.builders.generator.api.JavaFileGenerator",
    date = "3333-03-13T00:00Z[UTC]"
)
public class ClassWithGenericsBuilder<T> {
  private ClassWithGenerics objectToBuild;

  private final CallSetterFor callSetterFor = new CallSetterFor();

  private final FieldValue fieldValue = new FieldValue();

  private ClassWithGenericsBuilder(final ClassWithGenerics objectToBuild) {
    this.objectToBuild = objectToBuild;
  }

  public static ClassWithGenericsBuilder newInstance() {
    return new ClassWithGenericsBuilder(null);
  }

  public static ClassWithGenericsBuilder thatModifies(final ClassWithGenerics objectToModify) {
    Objects.requireNonNull(objectToModify);
    return new ClassWithGenericsBuilder(objectToModify);
  }

  public ArrayFloats floats() {
    return new ArrayFloats();
  }

  public ClassWithGenericsBuilder anInt(final int anInt) {
    fieldValue.anInt = anInt;
    callSetterFor.anInt = true;
    return this;
  }

  public ClassWithGenericsBuilder floats(final float[] floats) {
    fieldValue.floats = floats;
    callSetterFor.floats = true;
    return this;
  }

  public ClassWithGenericsBuilder t(final Object t) {
    fieldValue.t = t;
    callSetterFor.t = true;
    return this;
  }

  public ClassWithGenerics build() {
    if (objectToBuild == null) {
      objectToBuild = new ClassWithGenerics();
    }
    if (callSetterFor.anInt) {
      objectToBuild.setAnInt(fieldValue.anInt);
    }
    if (callSetterFor.floats) {
      objectToBuild.setFloats(fieldValue.floats);
    }
    if (callSetterFor.t) {
      objectToBuild.setT(fieldValue.t);
    }
    return objectToBuild;
  }

  private class CallSetterFor {
    boolean anInt;

    boolean floats;

    boolean t;
  }

  private class FieldValue {
    int anInt;

    float[] floats;

    Object t;
  }

  public class ArrayFloats {
    private List<Float> list;

    public ArrayFloats add(final float item) {
      if (this.list == null) {
        this.list = new ArrayList<>();
      }
      this.list.add(item);
      ClassWithGenericsBuilder.this.callSetterFor.floats = true;
      return this;
    }

    public ClassWithGenericsBuilder and() {
      if (this.list != null) {
        ClassWithGenericsBuilder.this.fieldValue.floats = new float[this.list.size()];
        for (int i = 0; i < this.list.size(); i++) {
          ClassWithGenericsBuilder.this.fieldValue.floats[i] = this.list.get(i);
        }
      }
      return ClassWithGenericsBuilder.this;
    }
  }
}
