/* (C) 2025 */
import com.github.emmmm9o.javatypes.*;

import mindustry.*;
import mindustry.core.*;
import mindustry.ui.*;
import mindustry.gen.*;
import arc.*;

/**
 * TestMain
 */

public class TestParser {


  public static void main(String[] args) {

    var parser = new Parser();
    parser.filter = clazz -> clazz.getName().contains("java") || clazz.getName().contains("rhino");
    parser.initEnv();
    parser.parse(Vars.class);
    parser.parse(Core.class);
    parser.parse(UI.class);
    parser.parse(Styles.class);
    parser.parse(Tex.class);
    parser.parse(Icon.class);
    parser.parse(World.class);
    parser.parse(Call.class);
    System.out.println(TestUtil.toJson(parser.classMap.values()));
  }
}
