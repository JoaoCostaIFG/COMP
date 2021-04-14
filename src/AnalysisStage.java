
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
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
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
        List<Reports> reports = parserResult.getReports();

        ImportVisitor importVisitor = new ImportVisitor(symbolTable);
        importVisitor.visit(node, reports);
        System.out.println("Imports: " + symbolTable.getImports());

        ClassVisitor classVisitor = new ClassVisitor(symbolTable);
        classVisitor.visit(node, reports);
        System.out.println("Class: " + symbolTable.getClassName() + " " + symbolTable.getSuper());

        ClassFieldVisitor classFieldVisitor = new ClassFieldVisitor(symbolTable);
        classFieldVisitor.visit(node, reports);
        System.out.println("Class fields:");
        for (Symbol s : symbolTable.getFields()) {
            Type t = s.getType();
            System.out.println("\t" + t.getName() + (t.isArray() ? "[]" : "") + " " + s.getName());
        }

        MethodVisitor methodVisitor = new MethodVisitor(symbolTable);
        methodVisitor.visit(node, reports);
        System.out.println("Methods:");
        for (String methodName : symbolTable.getMethods()) {
            Type returnType = symbolTable.getReturnType(methodName);
            System.out.println("\t" + returnType.getName() + (returnType.isArray() ? "[]" : "") + " " + methodName);
            for (Symbol param : symbolTable.getParameters(methodName)) {
                Type paramType = param.getType();
                System.out.println("\t\t" + paramType.getName() + (paramType.isArray() ? "[]" : "") + " " + param.getName());
            }
            System.out.println("\t\t-------------------------------");
            for (Symbol localVar : symbolTable.getLocalVariables(methodName)) {
                Type varType = localVar.getType();
                System.out.println("\t\t" + varType.getName() + (varType.isArray() ? "[]" : "") + " " + localVar.getName());
            }
        }

        System.out.println("Reports: " + reports);

        return new JmmSemanticsResult(parserResult, symbolTable, reports);
    }
}