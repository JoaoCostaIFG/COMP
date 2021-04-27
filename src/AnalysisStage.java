import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AnalysisStage implements JmmAnalysis {

    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {
        // abort analysis and throw a report in case the syntatic analysis has any errors
        if (TestUtils.getNumReports(parserResult.getReports(), ReportType.ERROR) > 0) {
            var errorReport = new Report(ReportType.ERROR, Stage.SEMANTIC, -1,
                    "Started semantic analysis but there are errors from previous stage");
            return new JmmSemanticsResult(parserResult, null, Collections.singletonList(errorReport));
        }

        // abort if there is no root node
        if (parserResult.getRootNode() == null) {
            var errorReport = new Report(ReportType.ERROR, Stage.SEMANTIC, -1,
                    "Started semantic analysis but AST root node is null");
            return new JmmSemanticsResult(parserResult, null, Collections.singletonList(errorReport));
        }

        MySymbolTable symbolTable = new MySymbolTable();
        JmmNode rootNode = parserResult.getRootNode();
        List<Report> reports = new ArrayList<>();

        // FILL SYMBOL TABLE
        ImportVisitor importVisitor = new ImportVisitor(symbolTable);
        importVisitor.visit(rootNode, reports);

        ClassVisitor classVisitor = new ClassVisitor(symbolTable);
        classVisitor.visit(rootNode, reports);

        ClassFieldVisitor classFieldVisitor = new ClassFieldVisitor(symbolTable);
        classFieldVisitor.visit(rootNode, reports);

        MethodVisitor methodVisitor = new MethodVisitor(symbolTable);
        methodVisitor.visit(rootNode, reports);

        // visit method bodies and do semantic analysis
        for (String methodName : symbolTable.getMethods()) {
            Method method = symbolTable.getMethod(methodName);
            BodyVisitor bodyVisitor = new BodyVisitor(symbolTable, method, methodName);
            bodyVisitor.visit(method.getNode(), reports);
            StaticVisitor staticVisitor = new StaticVisitor(symbolTable);
            staticVisitor.visit(method.getNode());
        }

        // System.out.println("Reports: " + reports);
        return new JmmSemanticsResult(parserResult, symbolTable, reports);
    }
}