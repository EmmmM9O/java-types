import com.github.emmmm9o.javatypes.*;

import arc.*;

public class TestTSGenerator{
  public static void main(String[] args) {

    var parser = new Parser();
    parser.filter = clazz -> clazz.getName().contains("java") || clazz.getName().contains("rhino");
    parser.initEnv();
    parser.parse(Core.class);
    // System.out.println(TestUtil.toJson(parser.classMap.values()));
    var generator = new TSGenerator();
    System.out.println(generator.generate(parser.classMap));
  }

}
