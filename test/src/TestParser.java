
/* (C) 2025 */
import java.util.*;

import com.github.emmmm9o.javatypes.*;
import com.github.emmmm9o.javatypes.JavaTypes.*;

import arc.util.serialization.*;
import arc.util.serialization.Json.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.ui.*;
import mindustry.gen.*;
import arc.*;

/**
 * TestMain
 */

public class TestParser {

  public static class JavaUseTypeJson {
    public Object type;
    public List<JavaTypeUse> generics;
    public List<String> upper;
    public List<String> lower;
    public String typeRef;
    public boolean array;
  }

  public static Json json = new Json(JsonWriter.OutputType.json);
  public static List<String> getNames(List<JavaTypeUse> list){
	  if(list==null) return null;
    var arr=new ArrayList<String>();
    for(var obj:list){
      arr.add(obj.typeRef.toString());
    }
    return arr;
  }
  public static void main(String[] args) {
	  json.setTypeName(null);
    json.setSerializer(Set.class, new Serializer<>() {
      @Override
      public void write(Json json, Set obj, Class knownType) {
        json.writeValue(new ArrayList(obj));
      }

      @Override
      public Set read(Json arg0, JsonValue arg1, Class arg2) {
        return null;
      }
    });
    json.setSerializer(JavaModifier.class, new Serializer<>() {
      @Override
      public void write(Json json, JavaModifier obj, Class knownType) {
        json.writeValue(obj.toString().toLowerCase());
      }

      @Override
      public JavaModifier read(Json arg0, JsonValue arg1, Class arg2) {
        return null;
      }
    });
    json.setSerializer(JavaTypeUse.class, new Serializer<>() {
      @Override
      public void write(Json json, JavaTypeUse obj, Class knownType) {
        json.writeValue(new JavaUseTypeJson() {
          {
            if (obj.typeC != null) {
              type = obj.typeC;
              array = true;
            }
            if (obj.type != null)
              type = obj.type.name;
            if (obj.typeG != null)
              type = obj.typeG;
            typeRef = obj.typeRef.getClass().toString();
            generics = obj.generics;
            upper = getNames(obj.upper);
            lower = getNames(obj.lower);
          }
        });
      }

      @Override
      public JavaTypeUse read(Json arg0, JsonValue arg1, Class arg2) {
        return null;
      }
    });
    json.setSerializer(Class.class, new Serializer<>() {
      @Override
      public void write(Json json, Class obj, Class knownType) {
        json.writeValue(obj.toString());
      }

      @Override
      public Class read(Json arg0, JsonValue arg1, Class arg2) {
        return null;
      }
    });
    var parser = new Parser();
    parser.filter=clazz->clazz.getName().contains("java")||clazz.getName().contains("rhino");
    parser.initEnv();
    parser.parse(Vars.class);
    parser.parse(Core.class);
    parser.parse(UI.class);
    parser.parse(Styles.class);
    parser.parse(Tex.class);
    parser.parse(Icon.class);
    parser.parse(World.class);
    parser.parse(Call.class);
    System.out.println(Jval.read(json.toJson(parser.classMap.values())).toString(Jval.Jformat.formatted));
  }
}
