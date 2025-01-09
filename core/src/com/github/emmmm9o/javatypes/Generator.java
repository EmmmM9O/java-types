/* (C) 2025 */
package com.github.emmmm9o.javatypes;

import java.util.*;

import com.github.emmmm9o.javatypes.JavaTypes.*;


public interface Generator {
  public String generate(Map<String, JavaType> maps, List<JavaType> values);
}
