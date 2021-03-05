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
    public void nachotest() {
        String jmmCode = SpecsIo.read("test/nachotest.jmm");
        System.out.println(TestUtils.parse(jmmCode).getRootNode());
    }

    @Test
    public void helloworldtest() {
        var jmmCode = SpecsIo.getResource("fixtures/public/HelloWorld.jmm");
        System.out.println(TestUtils.parse(jmmCode).getRootNode());
        //System.out.println(TestUtils.parse(jmmCode).getRootNode().toJson());
    }

    @Test
    public void life() {
        var jmmCode = SpecsIo.getResource("fixtures/public/Life.jmm");
        System.out.println(TestUtils.parse(jmmCode).getRootNode());
    }
}
