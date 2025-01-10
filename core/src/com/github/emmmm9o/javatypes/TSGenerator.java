/* (C) 2025 */
package com.github.emmmm9o.javatypes;

import java.util.*;

import com.github.emmmm9o.javatypes.JavaTypes.*;

public class TSGenerator implements Generator {
  public String getModulePath(JavaType type) {
    String obj =
        type.classpath.replace("$", ".").substring(0, type.classpath.length() - type.name.length());
    if (obj.endsWith("."))
      return obj.substring(0, obj.length() - 1);
    return obj;
  }

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
  {
    trefs.put("int", "number");
    trefs.put("float", "number");
    trefs.put("double", "number");
    trefs.put("void", "void");
    trefs.put("boolean", "boolean");
    trefs.put("char", "string");
    trefs.put("byte", "string");
    trefs.put("java.lang.String", "string");
    trefs.put("java.lang.Double", "number");
    trefs.put("java.lang.Float", "number");
    trefs.put("java.lang.Integer", "number");
    trefs.put("java.lang.Void", "void");
    trefs.put("java.lang.Boolean", "boolean");
    trefs.put("java.lang.Object", "any");

  }

  public String getType(JavaType type) {
    var t = trefs.getOrDefault(type.classpath, null);
    if (t != null)
      return t;
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
    str.append(generateMoifiers(field.modifiers))
        .append(field.name.replace("constructor", "_constructor")).append("?:")
        .append(generateTypeUse(field.type)).append(";\n");
    return str.toString();
  }

  public String generateParamater(JavaParamater paramater) {
    var str = new StringBuilder();
    str.append("_" + paramater.name).append(":").append(generateTypeUse(paramater.type))
        .append(" | null");
    return str.toString();
  }

  public String generateParamaterV(JavaParamater paramater) {
    var str = new StringBuilder();
    str.append("..._" + paramater.name).append(":").append(generateTypeUse(paramater.type));
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
    if (method.name.contains("$"))
      return "";
    var str = new StringBuilder();
    str.append(generateMoifiers(method.modifiers)).append(method.name)
        .append(generateGenerics(method.generics, true))
        .append(generateParamaters(method.paramaters, method.varArgs)).append(":")
        .append(generateTypeUse(method.result)).append(";\n");
    return str.toString();
  }

  public String generateType(JavaType type) {
    if (trefs.getOrDefault(type.classpath, null) != null)
      return "";// skip
    var str = new StringBuilder();
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
    str.append(generateGenerics(type.generics, true))
        .append(type.superType != null && type.superType.type != null
            && type.superType.type.classpath != "java.lang.Object"
                ? " extends " + generateTypeUse(type.superType)
                : "");
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
    String obj = getType(type);
    var split = obj.split("\\.");
    if (split.length != 0)
      split = Arrays.copyOf(split, split.length - 1);
    if (split.length != 0)
      for (var s : split) {
        if (!s.isEmpty())
          str.append("declare namespace ").append(s).append(" {\n");
      }
    if (!module.name.isEmpty())
      addSpaces(generateType(type), str);
    else
      str.append(generateType(type));
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

  @Override
  public String generate(Map<String, JavaType> map, List<JavaType> values) {
    this.map = map;
    initModules();
    var str = new StringBuilder();
    for (var v : values) {
      modifierJavaType(v);
      str.append(generateModule(getModule(getModulePath(v)), v));
    }
    for (var v : values) {
      var t = trefs.getOrDefault(v.classpath, null);
      if (t == null)
        str.append("declare const ").append(v.name).append(" = ").append(getType(v)).append(";\n");
    }
    return str.toString();
  }
}
