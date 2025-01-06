package com.github.emmmm9o.javatypes;

import java.util.*;

import com.github.emmmm9o.javatypes.JavaTypes.*;

public class TSGenerator implements Generator {
  public String getModulePath(JavaType type) {
    String obj = type.classpath.replace("$", ".").substring(0, type.classpath.length() - type.name.length());
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

  public Map<String, TSModule> refs = new HashMap<>();
  public TSModule root = new TSModule();

  public TSModule getModule(String module) {
    var t = refs.getOrDefault(module, null);
    if (t != null)
      return t;
    var now = root;
    for (var value : module.split("\\.")) {
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

  public static String addSpaces(String input) {
    String[] lines = input.split("\n");
    StringBuilder sb = new StringBuilder();

    for (String line : lines) {
      sb.append("  ").append(line).append("\n");
    }
    return sb.toString();

  }

  public String generateType(JavaType type) {
    var str = new StringBuilder();
    str.append("export ")
        .append(type.classRef.isEnum() ? "enum " : (type.classRef.isInterface() ? "interface " : "class "))
        .append(type.name).append("{\n");
    str.append("}\n");
    return str.toString();
  }

  public String generateModule(TSModule module) {
    var str = new StringBuilder();
    str.append("export module ").append(module.name)
        .append("{\n");
    for (var type : module.types.values()) {
      str.append(addSpaces(generateType(map.get(type))));
    }
    for (var sub : module.modules.values()) {
      str.append(addSpaces(generateModule(sub)));
    }
    str.append("}\n");
    return str.toString();
  }

  @Override
  public String generate(Map<String, JavaType> map) {
    this.map = map;
    initModules();
    return generateModule(root);
  }
}
