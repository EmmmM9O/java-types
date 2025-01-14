/* (C) 2025 */
package com.github.emmmm9o.javatypes;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core class containing type definitions for Java to other language type conversion.
 * This class provides the data structures needed to represent Java types, methods,
 * fields, and their relationships for type generation.
 */
public class JavaTypes {
  public enum JavaModifier {
    Public, Protected, Private, Final, Static, Abstract
  }

  /**
   * Represents a Java type with its structure and metadata.
   * This can be a class, interface, enum, or record.
   */
  public static class JavaType {
    private String name;
    private String classpath;
    private Class<?> classRef;
    private List<JavaTypeUse> generics;
    private Set<JavaModifier> modifiers;
    private JavaTypeUse superType;
    private Set<JavaTypeUse> interfaces;
    private List<JavaField> fields;
    private List<JavaMethod> methods;
    private List<JavaMethod> constructors;
    private List<JavaType> innerClasses;  // renamed from classes for clarity
    private boolean isInner;  // renamed from inner for clarity

    private JavaType() {
      this.generics = new ArrayList<>();
      this.modifiers = new HashSet<>();
      this.interfaces = new HashSet<>();
      this.fields = new ArrayList<>();
      this.methods = new ArrayList<>();
      this.constructors = new ArrayList<>();
      this.innerClasses = new ArrayList<>();
    }

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {
      private final JavaType type = new JavaType();

      public Builder withName(String name) {
        type.name = name;
        return this;
      }

      public Builder withClasspath(String classpath) {
        type.classpath = classpath;
        return this;
      }

      public Builder withClassRef(Class<?> classRef) {
        type.classRef = classRef;
        return this;
      }

      public Builder withGenerics(List<JavaTypeUse> generics) {
        type.generics = new ArrayList<>(generics);
        return this;
      }

      public Builder withModifiers(Set<JavaModifier> modifiers) {
        type.modifiers = new HashSet<>(modifiers);
        return this;
      }

      public Builder withSuperType(JavaTypeUse superType) {
        type.superType = superType;
        return this;
      }

      public Builder withInterfaces(Set<JavaTypeUse> interfaces) {
        type.interfaces = new HashSet<>(interfaces);
        return this;
      }

      public Builder withFields(List<JavaField> fields) {
        type.fields = new ArrayList<>(fields);
        return this;
      }

      public Builder withMethods(List<JavaMethod> methods) {
        type.methods = new ArrayList<>(methods);
        return this;
      }

      public Builder withConstructors(List<JavaMethod> constructors) {
        type.constructors = new ArrayList<>(constructors);
        return this;
      }

      public Builder withInnerClasses(List<JavaType> innerClasses) {
        type.innerClasses = new ArrayList<>(innerClasses);
        return this;
      }

      public Builder isInner(boolean isInner) {
        type.isInner = isInner;
        return this;
      }

      public JavaType build() {
        validate();
        return type;
      }

      private void validate() {
        if (type.name == null || type.name.isEmpty()) {
          throw new IllegalStateException("Type name cannot be null or empty");
        }
        if (type.classpath == null || type.classpath.isEmpty()) {
          throw new IllegalStateException("Classpath cannot be null or empty");
        }
        if (type.classRef == null) {
          throw new IllegalStateException("Class reference cannot be null");
        }
      }
    }

    // Getters
    public String getName() { return name; }
    public String getClasspath() { return classpath; }
    public Class<?> getClassRef() { return classRef; }
    public List<JavaTypeUse> getGenerics() { return Collections.unmodifiableList(generics); }
    public Set<JavaModifier> getModifiers() { return Collections.unmodifiableSet(modifiers); }
    public JavaTypeUse getSuperType() { return superType; }
    public Set<JavaTypeUse> getInterfaces() { return Collections.unmodifiableSet(interfaces); }
    public List<JavaField> getFields() { return Collections.unmodifiableList(fields); }
    public List<JavaMethod> getMethods() { return Collections.unmodifiableList(methods); }
    public List<JavaMethod> getConstructors() { return Collections.unmodifiableList(constructors); }
    public List<JavaType> getInnerClasses() { return Collections.unmodifiableList(innerClasses); }
    public boolean isInner() { return isInner; }

    /**
     * Creates a new JavaType instance from a Class object.
     * @param clazz The class to create the type from
     * @return A new JavaType instance
     */
    public static JavaType fromClass(Class<?> clazz) {
      return builder()
        .withName(clazz.getSimpleName())
        .withClasspath(clazz.getName())
        .withClassRef(clazz)
        .withModifiers(Parser.getModifiers(clazz.getModifiers()))
        .isInner(clazz.getDeclaringClass() != null)
        .build();
    }

    // Mutators (package-private, used by Parser)
    void setSuperType(JavaTypeUse superType) {
        this.superType = superType;
    }

    void addInterface(JavaTypeUse interfaceType) {
        if (interfaceType != null) {
            this.interfaces.add(interfaceType);
        }
    }

    void addField(JavaField field) {
        if (field != null) {
            this.fields.add(field);
        }
    }

    void addMethod(JavaMethod method) {
        if (method != null) {
            this.methods.add(method);
        }
    }

    void addConstructor(JavaMethod constructor) {
        if (constructor != null) {
            this.constructors.add(constructor);
        }
    }

    void addInnerClass(JavaType innerClass) {
        if (innerClass != null) {
            this.innerClasses.add(innerClass);
        }
    }
  }

  /**
   * Represents a use of a Java type, including generics and type variables.
   */
  public static class JavaTypeUse {
    private JavaType type;
    private String typeGeneric;  // renamed from typeG for clarity
    private JavaTypeUse componentType;  // renamed from typeC for clarity
    private List<JavaTypeUse> generics;
    private List<JavaTypeUse> upperBounds;  // renamed from upper for clarity
    private List<JavaTypeUse> lowerBounds;  // renamed from lower for clarity
    private Type typeRef;

    private JavaTypeUse() {
      this.generics = new ArrayList<>();
    }

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {
      private final JavaTypeUse typeUse = new JavaTypeUse();

      public Builder withType(JavaType type) {
        typeUse.type = type;
        return this;
      }

      public Builder withTypeGeneric(String typeGeneric) {
        typeUse.typeGeneric = typeGeneric;
        return this;
      }

      public Builder withComponentType(JavaTypeUse componentType) {
        typeUse.componentType = componentType;
        return this;
      }

      public Builder withGenerics(List<JavaTypeUse> generics) {
        typeUse.generics = new ArrayList<>(generics != null ? generics : Collections.emptyList());
        return this;
      }

      public Builder withUpperBounds(List<JavaTypeUse> upperBounds) {
        typeUse.upperBounds = upperBounds != null ? new ArrayList<>(upperBounds) : null;
        return this;
      }

      public Builder withLowerBounds(List<JavaTypeUse> lowerBounds) {
        typeUse.lowerBounds = lowerBounds != null ? new ArrayList<>(lowerBounds) : null;
        return this;
      }

      public Builder withTypeRef(Type typeRef) {
        typeUse.typeRef = typeRef;
        return this;
      }

      public JavaTypeUse build() {
        validate();
        return typeUse;
      }

      private void validate() {
        if (typeUse.type == null && typeUse.typeGeneric == null && typeUse.componentType == null) {
          throw new IllegalStateException("Type use must have either a type, type generic, or component type");
        }
        if (typeUse.typeRef == null) {
          throw new IllegalStateException("Type reference cannot be null");
        }
        if (typeUse.generics == null) {
          typeUse.generics = new ArrayList<>();
        }
      }
    }

    // Getters
    public JavaType getType() { return type; }
    public String getTypeGeneric() { return typeGeneric; }
    public JavaTypeUse getComponentType() { return componentType; }
    public List<JavaTypeUse> getGenerics() { return Collections.unmodifiableList(generics); }
    public List<JavaTypeUse> getUpperBounds() { return upperBounds != null ? Collections.unmodifiableList(upperBounds) : null; }
    public List<JavaTypeUse> getLowerBounds() { return lowerBounds != null ? Collections.unmodifiableList(lowerBounds) : null; }
    public Type getTypeRef() { return typeRef; }

    // Mutators for bounds (needed by Parser)
    void setUpperBounds(List<JavaTypeUse> bounds) {
        this.upperBounds = bounds != null ? new ArrayList<>(bounds) : null;
    }

    void setLowerBounds(List<JavaTypeUse> bounds) {
        this.lowerBounds = bounds != null ? new ArrayList<>(bounds) : null;
    }
  }

  /**
   * Represents a parameter in a method or constructor.
   */
  public static class JavaParameter {  // Fixed spelling
    private JavaTypeUse type;
    private boolean nullable;
    private String name;

    private JavaParameter() {}

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {
      private final JavaParameter parameter = new JavaParameter();

      public Builder withType(JavaTypeUse type) {
        parameter.type = type;
        return this;
      }

      public Builder isNullable(boolean nullable) {
        parameter.nullable = nullable;
        return this;
      }

      public Builder withName(String name) {
        parameter.name = name;
        return this;
      }

      public JavaParameter build() {
        validate();
        return parameter;
      }

      private void validate() {
        if (parameter.type == null) {
          throw new IllegalStateException("Parameter type cannot be null");
        }
        if (parameter.name == null || parameter.name.isEmpty()) {
          throw new IllegalStateException("Parameter name cannot be null or empty");
        }
      }
    }

    // Getters
    public JavaTypeUse getType() { return type; }
    public boolean isNullable() { return nullable; }
    public String getName() { return name; }
  }

  /**
   * Represents a Java method or constructor.
   */
  public static class JavaMethod {
    private List<JavaParameter> parameters;  // Fixed spelling
    private JavaTypeUse result;
    private String name;
    private Set<JavaModifier> modifiers;
    private List<JavaTypeUse> generics;
    private boolean varArgs;
    private boolean nullable;
    private Method methodRef;
    private Constructor<?> constructorRef;

    private JavaMethod() {
      this.parameters = new ArrayList<>();
      this.modifiers = new HashSet<>();
      this.generics = new ArrayList<>();
    }

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {
      private final JavaMethod method = new JavaMethod();

      public Builder withParameters(List<JavaParameter> parameters) {
        method.parameters = new ArrayList<>(parameters);
        return this;
      }

      public Builder withResult(JavaTypeUse result) {
        method.result = result;
        return this;
      }

      public Builder withName(String name) {
        method.name = name;
        return this;
      }

      public Builder withModifiers(Set<JavaModifier> modifiers) {
        method.modifiers = new HashSet<>(modifiers);
        return this;
      }

      public Builder withGenerics(List<JavaTypeUse> generics) {
        method.generics = new ArrayList<>(generics);
        return this;
      }

      public Builder isVarArgs(boolean varArgs) {
        method.varArgs = varArgs;
        return this;
      }

      public Builder isNullable(boolean nullable) {
        method.nullable = nullable;
        return this;
      }

      public Builder withMethodRef(Method methodRef) {
        method.methodRef = methodRef;
        return this;
      }

      public Builder withConstructorRef(Constructor<?> constructorRef) {
        method.constructorRef = constructorRef;
        return this;
      }

      public JavaMethod build() {
        validate();
        return method;
      }

      private void validate() {
        if (method.name == null && method.constructorRef == null) {
          throw new IllegalStateException("Method must have a name or be a constructor");
        }
        if (method.methodRef != null && method.constructorRef != null) {
          throw new IllegalStateException("Method cannot be both a method and a constructor");
        }
      }
    }

    // Getters
    public List<JavaParameter> getParameters() { return Collections.unmodifiableList(parameters); }
    public JavaTypeUse getResult() { return result; }
    public String getName() { return name; }
    public Set<JavaModifier> getModifiers() { return Collections.unmodifiableSet(modifiers); }
    public List<JavaTypeUse> getGenerics() { return Collections.unmodifiableList(generics); }
    public boolean isVarArgs() { return varArgs; }
    public boolean isNullable() { return nullable; }
    public Method getMethodRef() { return methodRef; }
    public Constructor<?> getConstructorRef() { return constructorRef; }
  }

  /**
   * Represents a Java field.
   */
  public static class JavaField {
    private JavaTypeUse type;
    private String name;
    private Set<JavaModifier> modifiers;
    private boolean nullable;

    private JavaField() {
      this.modifiers = new HashSet<>();
    }

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {
      private final JavaField field = new JavaField();

      public Builder withType(JavaTypeUse type) {
        field.type = type;
        return this;
      }

      public Builder withName(String name) {
        field.name = name;
        return this;
      }

      public Builder withModifiers(Set<JavaModifier> modifiers) {
        field.modifiers = new HashSet<>(modifiers);
        return this;
      }

      public Builder isNullable(boolean nullable) {
        field.nullable = nullable;
        return this;
      }

      public JavaField build() {
        validate();
        return field;
      }

      private void validate() {
        if (field.type == null) {
          throw new IllegalStateException("Field type cannot be null");
        }
        if (field.name == null || field.name.isEmpty()) {
          throw new IllegalStateException("Field name cannot be null or empty");
        }
      }
    }

    // Getters
    public JavaTypeUse getType() { return type; }
    public String getName() { return name; }
    public Set<JavaModifier> getModifiers() { return Collections.unmodifiableSet(modifiers); }
    public boolean isNullable() { return nullable; }
  }
}
