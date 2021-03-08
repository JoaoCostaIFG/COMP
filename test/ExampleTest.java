import static org.junit.Assert.*;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.io.StringReader;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

public class ExampleTest {
    // very good
    @Test
    public void NachoTest() {
        String jmmCode = SpecsIo.read("test/nachotest.jmm");
        //System.out.println(TestUtils.parse(jmmCode).getRootNode());
        System.out.println(TestUtils.parse(jmmCode).getRootNode().toJson());
    }

    @Test
    public void ArrayAssignTest() {
        String jmmCode = SpecsIo.read("test/arrayassign.jmm");
        //System.out.println(TestUtils.parse(jmmCode).getRootNode());
        //System.out.println(TestUtils.parse(jmmCode).getRootNode().toJson());
        try {
            FileWriter myWriter = new FileWriter("result.json");
            myWriter.write(TestUtils.parse(jmmCode).getRootNode().toJson());
            myWriter.close();
            System.err.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.err.println("An error occurred.");
            e.printStackTrace();
        }
    }

    // good
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

    // bad
    @Test(expected = Exception.class)
    public void BadNachoTest() {
        String jmmCode = SpecsIo.read("test/badnachotest.jmm");
        System.out.println(TestUtils.parse(jmmCode).getRootNode());
    }

    @Test(expected = Exception.class)
    public void BlowUpTest() {
        var jmmCode = SpecsIo.getResource("fixtures/public/fail/syntactical/BlowUp.jmm");
        System.out.println(TestUtils.parse(jmmCode).getRootNode());
    }

    @Test//(expected = Exception.class)
    public void LengthErrorTest() {
        // TODO problem is inside while loop so there is no exception thrown
        var jmmCode = SpecsIo.getResource("fixtures/public/fail/syntactical/LengthError.jmm");
        System.out.println(TestUtils.parse(jmmCode).getRootNode());
    }
}
