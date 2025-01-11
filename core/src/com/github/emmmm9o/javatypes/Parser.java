/* (C) 2025 */
package com.github.emmmm9o.javatypes;

import java.lang.reflect.*;
import java.util.*;

import com.github.emmmm9o.javatypes.JavaTypes.*;

public class Parser {

  public static interface Filter {
    public boolean filter(Class<?> clazz);
  }
  public static interface FieldNullable {
    public boolean check(Field field);
  }
  public static interface ParameterNullable {
    public boolean check(Parameter paramater);
  }
  public static interface MethodNullable {
    public boolean check(Method paramater);
  }

  public FieldNullable fnull = f -> true;
  public ParameterNullable pnull = p -> true;
  public MethodNullable mnull = m -> true;
  public Filter filter = clazz -> false;
  public Map<String, JavaType> classMap = new HashMap<>();
  public Map<String, Boolean> supMap = new HashMap<>();
  public List<JavaType> values = new ArrayList<>();
  public Map<Type, JavaTypeUse> typeMap = new HashMap<>();

  public void put(Class<?> clazz) {
    var tmp = new JavaType(clazz);
    classMap.put(clazz.getName(), tmp);
    supMap.put(clazz.getName(), true);
    // values.add(tmp);
  }

  public void initEnv() {
    put(Object.class);
    put(Integer.class);
    put(String.class);
    put(Class.class);
    put(Double.class);
    put(Float.class);
    put(Character.class);
    put(Void.class);
    put(Boolean.class);
    put(int.class);
    put(boolean.class);
    put(float.class);
    put(double.class);
    put(void.class);
    put(byte.class);
    put(char.class);
  }

  public Parser() {

  }

  public static Set<JavaModifier> getModifiers(int value) {
    var res = new HashSet<JavaModifier>();
    if (Modifier.isPublic(value))
      res.add(JavaModifier.Public);
    if (Modifier.isProtected(value))
      res.add(JavaModifier.Protected);
    if (Modifier.isPrivate(value))
      res.add(JavaModifier.Private);
    if (Modifier.isStatic(value))
      res.add(JavaModifier.Static);
    if (Modifier.isFinal(value))
      res.add(JavaModifier.Final);
    if (Modifier.isAbstract(value))
      res.add(JavaModifier.Abstract);
    return res;
  }

  public ArrayList<JavaTypeUse> parseType(Type type) {
    if (type != null && type instanceof ParameterizedType gsu) {
      return parseGenerics(gsu);
    }
    return new ArrayList<>();
  }

  public JavaTypeUse getType(Type utype) {
    return getType(utype, false);
  }

  public JavaTypeUse getType(Type utype, boolean sup) {
    var tmp = typeMap.getOrDefault(utype, null);
    if (tmp != null)
      return tmp;
    if (utype instanceof Class<?> tClass) {
      tmp = (new JavaTypeUse() {
        {
          if (tClass.isArray()) {
            typeC = getType(tClass.getComponentType(), sup);
            type = null;
          } else {
            type = parse(tClass, sup);
            typeC = null;
          }
          typeG = null;
          generics = new ArrayList<>();
          typeRef = utype;
        }
      });
    }
    if (utype instanceof ParameterizedType ptype) {
      tmp = (new JavaTypeUse() {
        {
          type = parse((Class<?>) ptype.getRawType(), sup);
          typeG = null;
          generics = parseGenerics(ptype);
          typeRef = utype;
        }
      });
    }
    if (utype instanceof TypeVariable tv) {
      tmp = (new JavaTypeUse() {
        {
          type = null;
          typeG = tv.getName();
          generics = new ArrayList<>();
          typeRef = utype;
        }
      });
      typeMap.put(utype, tmp);
      tmp.upper = getTypes(tv.getBounds());
    }
    if (utype instanceof GenericArrayType gat) {
      tmp = new JavaTypeUse() {
        {
          typeC = getType(gat.getGenericComponentType(), sup);
          typeG = null;
          type = null;
          generics = new ArrayList<>();
          typeRef = utype;
        }
      };
    }
    if (utype instanceof WildcardType wct) {
      tmp = new JavaTypeUse() {
        {
          type = null;
          typeG = "?";
          typeRef = utype;
        }
      };
      typeMap.put(utype, tmp);
      tmp.upper = getTypes(wct.getUpperBounds());
      tmp.lower = getTypes(wct.getLowerBounds());
    }
    typeMap.put(utype, tmp);
    if (tmp != null)
      return tmp;
    throw new RuntimeException("unkown type " + utype.getClass().toString());
  }

  public ArrayList<JavaTypeUse> getTypes(Type[] list) {
    var tmp = new ArrayList<JavaTypeUse>();
    for (Type type : list) {
      tmp.add(getType(type));
    }
    return tmp;
  }

  public ArrayList<JavaTypeUse> parseGenerics(ParameterizedType gsu) {
    return getTypes(gsu.getActualTypeArguments());
  }

  public JavaType parse(Class<?> clazz) {
    return parse(clazz, false);
  }

  public JavaType parse(Class<?> clazz, boolean sup) {
    var tmp = classMap.getOrDefault(clazz.getName(), null);
    var su = supMap.getOrDefault(clazz.getName(), false);
    if (su && sup) {
      supMap.put(clazz.getName(), false);
    } else if (tmp != null)
      return tmp;

    tmp = new JavaType(clazz);
    tmp.generics = getTypes(clazz.getTypeParameters());
    classMap.put(clazz.getName(), tmp);
    if (filter.filter(clazz) && !sup) {
      if (!tmp.inner)
        values.add(tmp);
      supMap.put(clazz.getName(), true);
      return tmp;
    }

    var superClass = clazz.getGenericSuperclass();
    if (superClass != null) {
      tmp.superType = getType(superClass, true);
    }
    for (var intf : clazz.getGenericInterfaces()) {
      tmp.interfaces.add(getType(intf, true));
    }
    for (var claz : clazz.getDeclaredClasses()) {
      tmp.classes.add(parse(claz));
    }
    for (var field : clazz.getDeclaredFields()) {
      tmp.fields.add(new JavaField() {
        {
          type = getType(field.getGenericType());
          name = field.getName();
          modifiers = getModifiers(field.getModifiers());
          nullable = fnull.check(field);
        }
      });
    }
    for (var method : clazz.getConstructors()) {
      tmp.constructors.add(new JavaMethod() {
        {
          varArgs = method.isVarArgs();
          modifiers = getModifiers(method.getModifiers());
          result = null;
          name = null;
          paramaters = new ArrayList<>();
          generics = getTypes(method.getTypeParameters());
          cref=method;
          var pt = method.getParameters();
          nullable = false;
          for (var p : pt) {
            paramaters.add(new JavaParamater() {
              {
                name = p.getName();
                type = getType(p.getParameterizedType());
                nullable = pnull.check(p);
              }
            });
          }
        }
      });
    }
    for (var method : clazz.getDeclaredMethods()) {
      tmp.methods.add(new JavaMethod() {
        {
          varArgs = method.isVarArgs();
          modifiers = getModifiers(method.getModifiers());
          result = getType(method.getGenericReturnType());
          name = method.getName();
          paramaters = new ArrayList<>();
          generics = getTypes(method.getTypeParameters());
          nullable = mnull.check(method);
          var pts = method.getGenericParameterTypes();
          var pt = method.getParameters();
          mref=method;
          for (var p : pt) {
            paramaters.add(new JavaParamater() {
              {
                name = p.getName();
                type = getType(p.getParameterizedType());
                nullable = pnull.check(p);
              }
            });
          }
        }
      });
    }
    if (!tmp.inner)
      values.add(tmp);
    return tmp;
  }
}
