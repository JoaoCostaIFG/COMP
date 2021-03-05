import static org.junit.Assert.*;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Properties;
import java.io.StringReader;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

public class ExampleTest {

    @Test
    public void helloworld() {
        //var jmmCode = SpecsIo.getResource("fixtures/public/HelloWorld.jmm");
        var jmmCode = SpecsIo.getResource("fixtures/public/MonteCarloPi.jmm");
        System.out.println(TestUtils.parse(jmmCode).getRootNode());
        //var fileContents = SpecsIo.read("./test.txt");
    }

    //@Test
    //public void olamundo() {
    //    var jmmCode = SpecsIo.getResource("fixtures/public/HelloWorld.jmm");
    //    System.out.println(TestUtils.parse(jmmCode).getRootNode().toJson());
    //    //var fileContents = SpecsIo.read("./test.txt");
    //}

    //@Test
    //public void testExpression() {
	//	assertEquals("Expression", TestUtils.parse("2+3\n").getRootNode().getKind());
	//}
}
