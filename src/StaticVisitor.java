import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.List;

import static java.lang.Integer.parseInt;

public class StaticVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    private final MySymbolTable symbolTable;
    private boolean isMain;

    public StaticVisitor(MySymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.isMain = false;

        addVisit("MethodDeclaration", this::visitMethodRoot);
        addVisit("Binary", this::visitDot);
    }

    private Boolean visitMethodRoot(JmmNode jmmNode, List<Report> reports) {
        System.out.println("Visiting method: " + jmmNode.get("methodName"));
        for (JmmNode child : jmmNode.getChildren()) {
            if (child.getKind().equals("MainParameter")) {
                this.isMain = true;
            }
        }
        return true;
    }

    private Boolean visitDot(JmmNode node, List<Report> reports) {
        if (!node.get("op").equals("DOT"))
            return true;

        JmmNode childLeft = node.getChildren().get(0);
        JmmNode childRight = node.getChildren().get(1);
        if (childRight.getKind().equals("Len"))
            return true;

        if (this.isMain && childLeft.get("type").equals("this")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    parseInt(childRight.get("line")), parseInt(childRight.get("col")),
                    "The 'this' keyword cannot be referenced from a static context."));
            return false;
        }
        return true;
    }
}
