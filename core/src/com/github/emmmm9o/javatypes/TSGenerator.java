package com.github.emmmm9o.javatypes;

import java.util.*;

import com.github.emmmm9o.javatypes.JavaTypes.*;

public class TSGenerator implements Generator {
  @Override
  public String generate(Map<Class<?>, JavaType> map) {
    var builder = new StringBuilder();
    for (var value : map.values()) {
      builder.append("export ");
      if (value.classRef.isInterface())
        builder.append("interface ");
      else if (value.classRef.isEnum())
        builder.append("enum ");
      else
        builder.append("class ");
    }
    return null;
  }
}
