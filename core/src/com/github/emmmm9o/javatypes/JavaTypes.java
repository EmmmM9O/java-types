/* (C) 2025 */
package com.github.emmmm9o.javatypes;

import java.lang.reflect.*;
import java.util.*;


public class JavaTypes {
  public enum JavaModifier {
    Public, Protected, Private, Final, Static, Abstract
  }

  public static class JavaType {
    public String name;
    public String classpath;
    public Class<?> classRef;
    public List<JavaTypeUse> generics;
    public Set<JavaModifier> modifiers;
    public JavaTypeUse superType;
    public Set<JavaTypeUse> interfaces;
    public List<JavaField> fields;
    public List<JavaMethod> methods;

    public JavaType() {

    }

    public JavaType(Class<?> clazz) {
      name = clazz.getSimpleName();
      classRef = clazz;
      classpath = clazz.getName();
      generics = new ArrayList<>();
      modifiers = Parser.getModifiers(clazz.getModifiers());
      interfaces = new HashSet<>();
      fields = new ArrayList<>();
      methods = new ArrayList<>();
      superType = null;
    }
  }

  public static class JavaTypeUse {
    public JavaType type;
    public String typeG;// if it is generic
    public JavaTypeUse typeC;//if it is a array
    public List<JavaTypeUse> generics;
    public List<JavaTypeUse> upper;
    public List<JavaTypeUse> lower;
    public Type typeRef;
  }

  public static class JavaParamater {
    public JavaTypeUse type;
    public String name;
  }

  public static class JavaMethod {
    public List<JavaParamater> paramaters;
    public JavaTypeUse result;
    public String name;
    public Set<JavaModifier> modifiers;
    public List<JavaTypeUse> generics;

  }

  public static class JavaField {
    public JavaTypeUse type;
    public String name;
    public Set<JavaModifier> modifiers;
  }
}
