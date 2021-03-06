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
        //System.out.println(TestUtils.parse(jmmCode).getRootNode().toJson());
    }

    @Test
    public void findMaximumTest() {
        var jmmCode = SpecsIo.getResource("fixtures/public/FindMaximum.jmm");
        System.out.println(TestUtils.parse(jmmCode).getRootNode());
    }

    @Test
    public void helloWorldTest() {
        var jmmCode = SpecsIo.getResource("fixtures/public/HelloWorld.jmm");
        System.out.println(TestUtils.parse(jmmCode).getRootNode());
    }

    @Test
    public void lazySortTest() {
        var jmmCode = SpecsIo.getResource("fixtures/public/Lazysort.jmm");
        System.out.println(TestUtils.parse(jmmCode).getRootNode());
    }

    @Test
    public void lifeTest() {
        var jmmCode = SpecsIo.getResource("fixtures/public/Life.jmm");
        System.out.println(TestUtils.parse(jmmCode).getRootNode());
    }

    @Test
    public void monteCarloPiTest() {
        var jmmCode = SpecsIo.getResource("fixtures/public/MonteCarloPi.jmm");
        System.out.println(TestUtils.parse(jmmCode).getRootNode());
    }

    @Test
    public void QuickSortTest() {
        var jmmCode = SpecsIo.getResource("fixtures/public/QuickSort.jmm");
        System.out.println(TestUtils.parse(jmmCode).getRootNode());
    }

    @Test
    public void SimpleTest() {
        var jmmCode = SpecsIo.getResource("fixtures/public/Simple.jmm");
        System.out.println(TestUtils.parse(jmmCode).getRootNode());
    }

    @Test
    public void TicTacToeTest() {
        var jmmCode = SpecsIo.getResource("fixtures/public/TicTacToe.jmm");
        System.out.println(TestUtils.parse(jmmCode).getRootNode());
    }

    @Test
    public void WhileAndIfTest() {
        var jmmCode = SpecsIo.getResource("fixtures/public/WhileAndIF.jmm");
        System.out.println(TestUtils.parse(jmmCode).getRootNode());
    }
}
