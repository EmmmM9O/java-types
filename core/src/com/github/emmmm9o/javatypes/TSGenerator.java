/* (C) 2025 */
package com.github.emmmm9o.javatypes;

import java.util.*;
import java.lang.reflect.*;

import com.github.emmmm9o.javatypes.JavaTypes.*;

public class TSGenerator implements Generator {
  public String getModulePath(JavaType type) {
    String obj =
        type.classpath.replace("$", ".").substring(0, type.classpath.length() - type.name.length());
    if (obj.endsWith("."))
      return obj.substring(0, obj.length() - 1);
    return obj;
  }

  public static interface Info<T> {
    String get(T t);
  }

  public Info<JavaMethod> minfo = m -> "";
  public Info<JavaMethod> coinfo = m -> "";
  public Info<JavaField> finfo = m -> "";
  public Info<JavaType> cinfo = m -> "";

  public static class TSModule {
    public Map<String, TSModule> modules;
    public Map<String, String> types;
    public String name;

    public TSModule() {
      name = "";
      modules = new HashMap<>();
      types = new HashMap<>();
    }

    public TSModule(String name) {
      this.name = name;
      modules = new HashMap<>();
      types = new HashMap<>();
    }
  }

  public String prefix = "";
  public Map<String, TSModule> refs = new HashMap<>();
  public TSModule root = new TSModule();

  public TSModule getModule(String module) {
    var t = refs.getOrDefault(module, null);
    if (t != null)
      return t;
    var now = root;
    for (var value : module.replace("function", "_function").split("\\.")) {
      var tmp = now.modules.getOrDefault(value, null);
      if (tmp != null) {
        now = tmp;
        continue;
      }
      tmp = new TSModule(value);
      now.modules.put(value, tmp);
      now = tmp;
    }
    refs.put(module, now);
    return now;
  }

  public void initModules() {
    refs.put("", root);
    for (var value : map.values()) {
      getModule(getModulePath(value)).types.put(value.name, value.classpath);
    }
  }

  public Map<String, JavaType> map;

  public static void addSpaces(String input, StringBuilder sb) {
    String[] lines = input.split("\n");
    for (String line : lines) {
      sb.append("  ").append(line).append("\n");
    }
  }

  public Map<String, String> trefs = new HashMap<>();
  public Map<String, Boolean> noG = new HashMap<>();
  public Map<String, String> cmap = new HashMap<>();
  public Map<String, String> addonmap = new HashMap<>();

  public void putR(String ref, String r) {
    putR(ref, r, false);
  }

  public void putR(String ref, String r, boolean g) {
    trefs.put(ref, r);
    noG.put(ref, g);
  }

  {
    putR("int", "number", true);
    putR("float", "number", true);
    putR("double", "number", true);
    putR("void", "void", true);
    putR("boolean", "boolean", true);
    putR("char", "string", true);
    putR("byte", "string", true);
    putR("java.lang.String", "string");
    putR("java.lang.Double", "number");
    putR("java.lang.Float", "number");
    putR("java.lang.Integer", "number");
    putR("java.lang.Void", "void");
    putR("java.lang.Boolean", "boolean");
    putR("java.lang.Object", "any");
    putR("java.lang.Character", "string");
    putR("java.lang.Runnable", "()=>void");
    cmap.put("java.lang.Runnable", "declare type Runnable=()=>void");
  }

  public String getType(JavaType type) {
    return getType(type, false);
  }

  public String getType(JavaType type, boolean U) {
    if (!U) {
      var t = trefs.getOrDefault(type.classpath, null);
      if (t != null)
        return t;
    }
    return (prefix + type.classpath).replace("$", ".").replace("function", "_function")
        .replace(".type", "._type").replaceAll("\\.\\d+", ".");
  }

  public String generateGenerics(List<JavaTypeUse> generics) {
    return generateGenerics(generics, false);
  }

  public String generateGenerics(List<JavaTypeUse> generics, boolean up) {
    var str = new StringBuilder();
    if (generics != null && !generics.isEmpty()) {
      str.append("<");
      for (var t : generics) {
        str.append(generateTypeUse(t, up)).append(",");
      }
      str.deleteCharAt(str.length() - 1);
      str.append(">");
    }
    return str.toString();
  }

  public String generateTypeUse(JavaTypeUse type) {
    return generateTypeUse(type, false);
  }

  public String generateTypeUse(JavaTypeUse type, boolean up) {
    var str = new StringBuilder();
    if (type.typeG == "?")
      str.append("any");
    else if (type.typeG != null)
      str.append(type.typeG);
    if (type.typeC != null)
      str.append(generateTypeUse(type.typeC, false)).append("[]");
    if (type.type != null)
      str.append(getType(type.type));
    if (type.type != null && type.generics.isEmpty() && !type.type.generics.isEmpty()) {
      str.append("<");
      for (var t : type.type.generics) {
        str.append("any,");
        str.deleteCharAt(str.length() - 1);
      }
      str.append(">");
    }
    str.append(generateGenerics(type.generics));
    if (type.upper != null && !type.upper.isEmpty()) {
      var tm = new ArrayList<JavaTypeUse>();
      for (var t : type.upper) {
        if (t.type != null && t.type.classpath == "java.lang.Object")
          continue;
        tm.add(t);
      }
      type.upper = tm;
    }
    if (type.upper != null && !type.upper.isEmpty()
        && (up || (type.typeG == null || type.typeG == null))) {
      str.append(" extends ");
      for (var t : type.upper) {
        str.append(generateTypeUse(t, false)).append("&");
      }
      str.deleteCharAt(str.length() - 1);
    }
    return str.toString();
  }

  public String generateMoifier(JavaModifier modifier) {
    if (modifier == JavaModifier.Final)
      return "";
    if (modifier == JavaModifier.Abstract)
      return "";
    return modifier.toString().toLowerCase();
  }

  public String generateMoifiers(Set<JavaModifier> modifier) {
    var str = new StringBuilder();
    if (modifier != null)
      for (var m : modifier) {
        str.append(generateMoifier(m)).append(" ");
      }
    return str.toString();
  }

  public String generateField(JavaField field) {
    var str = new StringBuilder();
    str.append("/*").append(finfo.get(field)).append("*/\n");
    str.append(generateMoifiers(field.modifiers))
        .append(field.name.replace("constructor", "_constructor"))
        .append(field.nullable ? "?:" : ":").append(generateTypeUse(field.type)).append(";\n");
    return str.toString();
  }

  public String generateParamater(JavaParamater paramater) {
    var str = new StringBuilder();
    str.append("_" + paramater.name).append(":").append(getPSuf(paramater))
        .append(generateTypeUse(paramater.type));
    if (paramater.nullable)
      str.append(" | null");
    return str.toString();
  }

  public String getPSuf(JavaParamater paramater) {
    return (paramater.type.type != null && paramater.type.type.classpath == "java.lang.Class"
        ? ("Class" + (paramater.type.generics.isEmpty() ? ""
            : ("<" + generateTypeUse(paramater.type.generics.get(0)) + ">")) + " | ")
        : "");
  }

  public String generateParamaterV(JavaParamater paramater) {
    var str = new StringBuilder();
    str.append("..._" + paramater.name).append(":").append(getPSuf(paramater))
        .append(generateTypeUse(paramater.type));
    return str.toString();
  }

  public String generateParamaters(List<JavaParamater> paramaters, boolean va) {
    var str = new StringBuilder();
    str.append("(");
    for (int i = 0; i < paramaters.size(); i++) {
      JavaParamater p = paramaters.get(i);
      if (!va || i != paramaters.size() - 1)
        str.append(generateParamater(p)).append(",");
      else
        str.append(generateParamaterV(p)).append(",");
    }
    if (!paramaters.isEmpty())
      str.deleteCharAt(str.length() - 1);
    str.append(")");
    return str.toString();
  }


  public String generateMethod(JavaMethod method) {

    if (method.name != null && method.name.contains("$"))
      return "";
    var str = new StringBuilder();
    str.append("/*");
    if (method.name == null)
      str.append(coinfo.get(method));
    else
      str.append(minfo.get(method));
    str.append("*/\n").append(generateMoifiers(method.modifiers))
        .append(method.name == null ? "constructor" : method.name)
        .append(generateGenerics(method.generics, true))
        .append(generateParamaters(method.paramaters, method.varArgs));
    if (method.result != null) {
      str.append(":").append(generateTypeUse(method.result));
      if (method.result.type == null || method.result.type.name != "void" && method.nullable)
        str.append("|null");
    }
    str.append(";\n");
    return str.toString();
  }

  public String generateType(JavaType type) {
    if (noG.getOrDefault(type.classpath, false))
      return "";// skip
    var str = new StringBuilder();
    str.append("/*").append(cinfo.get(type)).append("*/\n");
    if (!type.inner)
      str.append("declare ")
          .append(type.modifiers.contains(JavaModifier.Abstract) ? "abstract " : "").append(
              /*
               * type.classRef.isEnum() ? "enum " : (type.classRef.isInterface() ? "interface " :
               */"class ");
    else {
      str.append(generateMoifiers(type.modifiers)).append(type.name).append("= class ");
    }

    if (!type.inner)
      str.append(type.name);
    str.append(generateGenerics(type.generics, true)).append(type.superType != null
        && type.superType.type != null && type.superType.type.classpath != "java.lang.Object"
            ? " extends " + generateTypeUse(type.superType)
            : (type.superType != null && type.superType.type != null
                && type.superType.type.classpath == "java.lang.Object" ? " extends java.lang.Object"
                    : " "));
    if (type.interfaces != null & !type.interfaces.isEmpty()) {
      str.append(" implements ");
      for (var i : type.interfaces) {
        str.append(generateTypeUse(i)).append(",");
      }

      str.deleteCharAt(str.length() - 1);
    }
    str.append(" {\n");
    for (var c : type.classes) {
      addSpaces(generateType(c), str);
    }
    for (var field : type.fields) {
      str.append("  ").append(generateField(field));
    }
    for (var method : type.methods) {
      str.append("  ").append(generateMethod(method));
    }
    for (var method : type.constructors) {
      str.append("  ").append(generateMethod(method));
    }
    str.append("}\n");
    return str.toString();
  }

  public String generateModule(TSModule module) {
    var str = new StringBuilder();
    if (!module.name.isEmpty())
      str.append("declare namespace ").append(module.name).append(" {\n");
    for (var sub : module.modules.values()) {
      if (!module.name.isEmpty())
        addSpaces(generateModule(sub), str);
      else
        str.append(generateModule(sub));
      str.append("\n");
    }
    for (var type : module.types.values()) {
      if (!module.name.isEmpty())
        addSpaces(generateType(map.get(type)), str);
      else
        str.append(generateType(map.get(type)));
      str.append("\n");
    }

    if (!module.name.isEmpty())
      str.append("}\n");
    return str.toString();
  }

  public String generateModule(TSModule module, JavaType type) {
    var str = new StringBuilder();
    String obj = getType(type, true);
    var split = obj.split("\\.");
    if (split.length != 0)
      split = Arrays.copyOf(split, split.length - 1);
    if (split.length != 0)
      for (var s : split) {
        if (!s.isEmpty())
          str.append("declare namespace ").append(s).append(" {\n");
      }
    var su = cmap.getOrDefault(type.classpath, null);
    if (!module.name.isEmpty())
      addSpaces(su == null ? generateType(type) : su, str);
    else
      str.append(su == null ? generateType(type) : su);
    for (var s : split) {
      if (!s.isEmpty())
        str.append("}\n");
    }
    return str.toString();
  }

  public void modifierMethod(JavaMethod method) {

  }

  public Map<String, Boolean> usedName = new HashMap<>();

  public void modifierJavaType(JavaType type) {
    usedName.clear();
    for (var m : type.methods) {
      modifierMethod(m);
      usedName.put(m.name, true);
    }
    var tmp = new ArrayList<JavaField>();
    for (var f : type.fields) {
      if (usedName.getOrDefault(f.name, false)) {
        continue;
      }
      tmp.add(f);
    }
    type.fields = tmp;
  }

  public String coustom = "declare type Class<T> = new (...args: any[]) => T;\n";

  @Override
  public String generate(Map<String, JavaType> map, List<JavaType> values) {
    this.map = map;
    initModules();
    var str = new StringBuilder();
    str.append(coustom);
    for (var v : values) {
      modifierJavaType(v);
      str.append(generateModule(getModule(getModulePath(v)), v));
    }
    for (var v : values) {
      var t = trefs.getOrDefault(v.classpath, null);
      if (t == null)
        str.append("declare const ").append(v.name).append(" = ").append(getType(v,true)).append(";\n");
      str.append("declare type ").append(v.name).append(" = ").append(getType(v,true)).append(";\n");
    }
    return str.toString();
  }
}
