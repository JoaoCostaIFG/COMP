import static org.junit.Assert.*;

import org.junit.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.SpecsIo;

public class SyntaticTests {
    /*
     *  helper
     */
    private JmmParserResult goodTest(String jmmCode) {
        JmmParserResult result = TestUtils.parse(jmmCode);
        List<Report> reports = result.getReports();
        TestUtils.noErrors(reports);

        return result;
    }

    private JmmParserResult badTest(String jmmCode, int numErrors) {
        JmmParserResult result = TestUtils.parse(jmmCode);
        List<Report> reports = result.getReports();
        TestUtils.mustFail(reports);
        assertEquals(TestUtils.getNumErrors(reports), numErrors);

        return result;
    }

    private void outputJsonTest(String filePath, JmmParserResult result) {
        try {
            FileWriter myWriter = new FileWriter(filePath);
            myWriter.write(result.toJson());
            myWriter.close();
            System.err.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.err.println("An error occurred.");
            e.printStackTrace();
        }
    }

    /*
     *  good
     */
    @Test
    public void NachoTest() {
        goodTest(SpecsIo.read("test/nachotest.jmm"));
    }

    @Test
    public void findMaximumTest() {
        goodTest(SpecsIo.getResource("fixtures/public/FindMaximum.jmm"));
    }

    @Test
    public void helloWorldTest() {
        goodTest(SpecsIo.getResource("fixtures/public/HelloWorld.jmm"));
    }

    @Test
    public void lazySortTest() {
        goodTest(SpecsIo.getResource("fixtures/public/Lazysort.jmm"));
    }

    @Test
    public void lifeTest() {
        goodTest(SpecsIo.getResource("fixtures/public/Life.jmm"));
    }

    @Test
    public void monteCarloPiTest() {
        goodTest(SpecsIo.getResource("fixtures/public/MonteCarloPi.jmm"));
    }

    @Test
    public void QuickSortTest() {
        goodTest(SpecsIo.getResource("fixtures/public/QuickSort.jmm"));
    }

    @Test
    public void SimpleTest() {
        goodTest(SpecsIo.getResource("fixtures/public/Simple.jmm"));
    }

    @Test
    public void TicTacToeTest() {
        goodTest(SpecsIo.getResource("fixtures/public/TicTacToe.jmm"));
    }

    @Test
    public void WhileAndIfTest() {
        goodTest(SpecsIo.getResource("fixtures/public/WhileAndIF.jmm"));
    }

    @Test
    public void TuringTest() {
        System.out.println("Weird static import thingy.");
    }

    @Test
    public void TicTacToeJavaTest() {
        System.out.println("The grammar doesn't accept this file (it's java not jmm).");
    }

    /*
     *  bad
     */
    @Test
    public void BadNachoTest() {
        String jmmCode = SpecsIo.read("test/badnachotest.jmm");
        TestUtils.parse(jmmCode);
    }

    @Test
    public void BlowUpTest() {
        var jmmCode = SpecsIo.getResource("fixtures/public/fail/syntactical/BlowUp.jmm");
        TestUtils.parse(jmmCode);
    }

    @Test
    public void CompleteWhileTest() {
        JmmParserResult result = badTest(SpecsIo.getResource("fixtures/public/fail/syntactical/CompleteWhileTest.jmm"), 11);
        outputJsonTest("result.json", result);
    }

    @Test
    public void LengthErrorTest() {
        badTest(SpecsIo.getResource("fixtures/public/fail/syntactical/LengthError.jmm"), 1);
    }

    @Test
    public void MissingRightParTest() {
        badTest(SpecsIo.getResource("fixtures/public/fail/syntactical/MissingRightPar.jmm"), 1);
    }

    @Test
    public void MultipleSequencialTest() {
        badTest(SpecsIo.getResource("fixtures/public/fail/syntactical/MultipleSequential.jmm"), 2);
    }

    @Test
    public void NestedLoopTest() {
        badTest(SpecsIo.getResource("fixtures/public/fail/syntactical/NestedLoop.jmm"), 2);
    }
}
