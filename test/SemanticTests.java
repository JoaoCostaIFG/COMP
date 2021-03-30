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
     *  good
     */
    @Test
    public void NachoTest() {
        String jmmCode = SpecsIo.read("test/nachotest.jmm");
        JmmParserResult result = TestUtils.parse(jmmCode);

        System.out.println(result.getRootNode().toJson());

        List<Report> reports = result.getReports();
        TestUtils.noErrors(reports);

        AnalysisStage analysisStage = new AnalysisStage();
        JmmSemanticsResult jmmSemanticsResult = analysisStage.semanticAnalysis(result);
    }

    /*
     *  bad
     */
//    @Test
//    public void BadNachoTest() {
//        String jmmCode = SpecsIo.read("test/badnachotest.jmm");
//        TestUtils.parse(jmmCode);
//    }
}
