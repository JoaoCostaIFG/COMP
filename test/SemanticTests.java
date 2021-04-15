import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.SpecsIo;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SemanticTests {
    /*
     *  helper
     */
    private JmmParserResult goodTest(String jmmCode) {
        JmmParserResult result = TestUtils.parse(jmmCode);
        AnalysisStage analysisStage = new AnalysisStage();
        JmmSemanticsResult jmmSemanticsResult = analysisStage.semanticAnalysis(result);
        List<Report> reports = jmmSemanticsResult.getReports();
        TestUtils.noErrors(reports);

        return result;
    }

    private JmmParserResult badTest(String jmmCode, int numErrors) {
        JmmParserResult result = TestUtils.parse(jmmCode);
        AnalysisStage analysisStage = new AnalysisStage();
        JmmSemanticsResult jmmSemanticsResult = analysisStage.semanticAnalysis(result);
        List<Report> reports = jmmSemanticsResult.getReports();
        TestUtils.mustFail(reports);
        assertEquals(TestUtils.getNumErrors(reports), numErrors);

        return result;
    }

    /*
     *  good
     */
    @Test
    public void NachoTest() {
        String jmmCode = SpecsIo.read("test/nachotest.jmm");
        JmmParserResult result = TestUtils.parse(jmmCode);
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
    public void arrIndexNotInt(){
        badTest(SpecsIo.getResource("fixtures/public/fail/semantic/arr_index_not_int.jmm"), 1);
    }

    @Test
    public void arrSizeNotInt(){
        badTest(SpecsIo.getResource("fixtures/public/fail/semantic/arr_size_not_int.jmm"), 1);
    }

    @Test
    public void badArguments(){
        badTest(SpecsIo.getResource("fixtures/public/fail/semantic/badArguments.jmm"), 2);
    }

    @Test
    public void binopIncomp(){
        badTest(SpecsIo.getResource("fixtures/public/fail/semantic/binop_incomp.jmm"), 1);
    }

    @Test
    public void funcNotFound(){
        badTest(SpecsIo.getResource("fixtures/public/fail/semantic/funcNotFound.jmm"), 1);
    }

    @Test
    public void simpleLength(){
        badTest(SpecsIo.getResource("fixtures/public/fail/semantic/simple_length.jmm"), 1);
    }

    @Test
    public void varExpIncomp(){
        badTest(SpecsIo.getResource("fixtures/public/fail/semantic/var_exp_incomp.jmm"), 1);
    }

    @Test
    public void varLitIncomp(){
        badTest(SpecsIo.getResource("fixtures/public/fail/semantic/var_lit_incomp.jmm"), 1);
    }

    @Test
    public void varUndef(){
        badTest(SpecsIo.getResource("fixtures/public/fail/semantic/var_undef.jmm"), 1);
    }

    @Test
    public void varNotInit(){
        badTest(SpecsIo.getResource("fixtures/public/fail/semantic/varNotInit.jmm"), 1);
    }
}
