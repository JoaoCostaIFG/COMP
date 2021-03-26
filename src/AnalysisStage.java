
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.examples.ExamplePostorderVisitor;
import pt.up.fe.comp.jmm.ast.examples.ExamplePreorderVisitor;
import pt.up.fe.comp.jmm.ast.examples.ExamplePrintVariables;
import pt.up.fe.comp.jmm.ast.examples.ExampleVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

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
        JmmNode node = parserResult.getRootNode();

        ImportVisitor importVisitor = new ImportVisitor(symbolTable);
        importVisitor.visit(node, parserResult.getReports());
        System.out.println("Imports: " + symbolTable.getImports());

        ClassVisitor classVisitor = new ClassVisitor(symbolTable);
        classVisitor.visit(node, parserResult.getReports());
        System.out.println("Class: " + symbolTable.getClassName() + " " + symbolTable.getSuper());

//        System.out.println("Dump tree with Visitor where you control tree traversal");
//        ExampleVisitor visitor = new ExampleVisitor("Identifier", "id");
//        System.out.println(visitor.visit(node, ""));
//
//        System.out.println("Dump tree with Visitor that automatically performs preorder tree traversal");
//        var preOrderVisitor = new ExamplePreorderVisitor("Identifier", "id");
//        System.out.println(preOrderVisitor.visit(node, ""));
//
//        System.out.println(
//                "Create histogram of node kinds with Visitor that automatically performs postorder tree traversal");
//        var postOrderVisitor = new ExamplePostorderVisitor();
//        var kindCount = new HashMap<String, Integer>();
//        postOrderVisitor.visit(node, kindCount);
//        System.out.println("Kinds count: " + kindCount + "\n");
//
//        System.out.println(
//                "Print variables name and line, and their corresponding parent with Visitor that automatically performs preorder tree traversal");
//        var varPrinter = new ExamplePrintVariables("Variable", "name", "line");
//        varPrinter.visit(node, null);

        return new JmmSemanticsResult(parserResult, symbolTable, new ArrayList<>());
    }
}