import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.SpecsIo;

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
        // System.err.println(result.getRootNode().toJson());
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
        // System.out.println(result.getRootNode().toJson());
        goodTest(jmmCode); // calma jovem tava a meter os testes :ok_hand:
    }

    /*
     *  bad
     */
    @Test
    public void KazengaTest() {
        String jmmCode = SpecsIo.read("test/testedokazenga.jmm");
        goodTest(jmmCode);
    }

    @Test
    public void ArrIndexNotIntTest() {
        badTest(SpecsIo.getResource("fixtures/public/fail/semantic/arr_index_not_int.jmm"), 1);
    }

    @Test
    public void ArrSizeNotIntTest() {
        badTest(SpecsIo.getResource("fixtures/public/fail/semantic/arr_size_not_int.jmm"), 1);
    }

    @Test
    public void BadArgumentsTest() {
        badTest(SpecsIo.getResource("fixtures/public/fail/semantic/badArguments.jmm"), 3);
    }

    @Test
    public void BinopIncompTest() {
        badTest(SpecsIo.getResource("fixtures/public/fail/semantic/binop_incomp.jmm"), 1);
    }

    @Test
    public void FuncNotFoundTest() {
        badTest(SpecsIo.getResource("fixtures/public/fail/semantic/funcNotFound.jmm"), 2);
    }

    @Test
    public void SimpleLengthTest() {
        badTest(SpecsIo.getResource("fixtures/public/fail/semantic/simple_length.jmm"), 1);
    }

    @Test
    public void VarExpIncompTest() {
        badTest(SpecsIo.getResource("fixtures/public/fail/semantic/var_exp_incomp.jmm"), 1);
    }

    @Test
    public void VarLitIncompTest() {
        badTest(SpecsIo.getResource("fixtures/public/fail/semantic/var_lit_incomp.jmm"), 1);
    }

    @Test
    public void VarUndefTest() {
        badTest(SpecsIo.getResource("fixtures/public/fail/semantic/var_undef.jmm"), 1);
    }

    @Test
    public void VarNotInitTest() {
        badTest(SpecsIo.read("test/varNotInit.jmm"), 1);
    }
}
