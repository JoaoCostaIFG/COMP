package Analysis.Visitors;

import Analysis.SymbolTable.MySymbolTable;
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Integer.parseInt;

public class ClassFieldVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    private int nodesFoundNo = 0;
    private final MySymbolTable symbolTable;

    public ClassFieldVisitor(MySymbolTable symbolTable) {
        super();
        this.symbolTable = symbolTable;
        addVisit("ClassFields", this::parseClassFields);
    }

    private Boolean parseClassFields(JmmNode node, List<Report> reports) {
        if (this.nodesFoundNo >= 1) {
            reports.add(new Report(ReportType.WARNING, Stage.SEMANTIC,
                    parseInt(node.get("line")), parseInt(node.get("col")),
                    "Found more than one class field code section. This shouldn't happen."));
            return false;
        }

        List<Symbol> classFields = new ArrayList<>();
        for (var child : node.getChildren()) {
            if (!child.getKind().equals("VarDeclaration")) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                        parseInt(child.get("line")), parseInt(child.get("col")),
                        "This should be a variable declaration. Only variable declarations are allowed on the class parameters section."));
            } else {
                Symbol varDeclaration = this.parseVarDeclaration(child);
                // TODO error checking (type is valid for declaration?)
                classFields.add(varDeclaration);
            }
        }

        this.symbolTable.setFields(classFields);
        ++this.nodesFoundNo;
        return true;
    }

    private Symbol parseVarDeclaration(JmmNode node) {
        JmmNode typeNode = node.getChildren().get(0);
        Type type = new Type(typeNode.get("dataType"), typeNode.get("isArray").equals("yes"));

        return new Symbol(type, node.get("varName"));
    }
}
