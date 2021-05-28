import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.List;

import static java.lang.Integer.parseInt;

public class StaticVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    private final MySymbolTable symbolTable;
    private final Method method;

    public StaticVisitor(MySymbolTable symbolTable, Method method) {
        this.symbolTable = symbolTable;
        this.method = method;

        addVisit("Binary", this::visitDot);
        addVisit("Literal", this::visitLiteral);
        addVisit("Var", this::visitAssignment);
    }

    private Boolean visitDot(JmmNode node, List<Report> reports) {
        if (!node.get("op").equals("DOT"))
            return true;

        JmmNode childLeft = node.getChildren().get(0);
        JmmNode childRight = node.getChildren().get(1);
        if (childRight.getKind().equals("Len"))
            return true;

        if (this.method.isMain() && childLeft.get("type").equals("this")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    parseInt(childRight.get("line")), parseInt(childRight.get("col")),
                    "The 'this' keyword cannot be referenced from a static context."));
            return false;
        }
        return true;
    }

    private boolean checkByVarName(JmmNode node, String varName, List<Report> reports) {
        // local vars
        for (Symbol s : this.method.getLocalVars()) {
            if (s.getName().equals(varName))
                return true;
        }
        // method parameters
        for (Symbol s : this.method.getParameters()) {
            if (s.getName().equals(varName))
                return true;
        }

        // has to be class field
        for (Symbol s : this.symbolTable.getFields()) {
            if (s.getName().equals(varName)) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                        parseInt(node.get("line")), parseInt(node.get("col")),
                        "The '" + varName + "' variable cannot be referenced from a static context (it's a class field)."));
                return false;
            }
        }

        return true;
    }

    private Boolean visitLiteral(JmmNode node, List<Report> reports) {
        if (!node.get("type").equals("identifier") || !this.method.isMain())
            return true;

        return this.checkByVarName(node, node.get("name"), reports);
    }

    private Boolean visitAssignment(JmmNode node, List<Report> reports) {
        if (!this.method.isMain())
            return true;

        return this.checkByVarName(node, node.get("name"), reports);
    }
}
