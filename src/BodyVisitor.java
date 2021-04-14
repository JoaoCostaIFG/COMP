import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.List;

import static java.lang.Integer.parseInt;

public class BodyVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    private final MySymbolTable symbolTable;
    private final Method method;

    public BodyVisitor(MySymbolTable symbolTable, Method method) {
        super();
        this.symbolTable = symbolTable;
        this.method = method;
        addVisit("Binary", this::visitBinary);
        addVisit("Unary", this::visitUnary);
    }

    private Boolean visitBinary(JmmNode node, List<Report> reports) {
        String op = node.get("op");
        switch (op) {
            case "AND":
            case "LESSTHAN":
                return this.validateBoolean(node, reports);
            case "ADD":
                break;
            case "SUB":
                break;
            case "MULT":
                break;
            case "DIV":
                break;
            case "INDEX":
                break;
            case "DOT":
                break;
        }

        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")), "Unknown operation."));
        return false;
    }

    private Boolean visitUnary(JmmNode node, List<Report> reports) {
        JmmNode child = node.getChildren().get(0);
        switch (child.getKind()) {
            case "Literal":
                if (!child.get("type").equals("bool")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")), "Not operator operand is not a boolean."));
                    return false;
                }
                break;
            case "Unary":
                // is unary => is of the NOT kind => boolean (validated by visit unary)
                break;
            case "Binary":
                if (!child.get("op").equals("AND") && !child.get("op").equals("LESSTHAN")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")), "Not operator operand is not a boolean."));
                    return false;
                }
                break;
            default:
                break;
        }

        return true;
    }

    private boolean validateBoolean(JmmNode node, List<Report> reports) {
        if (node.getNumChildren() != 2) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")), node.getKind() + " operator needs 2 operands."));
            return false;
        }

        JmmNode childLeft = node.getChildren().get(0);
        switch (childLeft.getKind()) {
            case "Literal":
                Symbol childLeftVar = method.getVar(childLeft.get("name"));
                if ((childLeftVar == null && !childLeft.get("type").equals("bool")) ||
                        (childLeftVar != null && !childLeftVar.getType().getName().equals("boolean"))) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")), node.getKind() + "  operator left operand is not a boolean."));
                    return false;
                }
                break;
            case "Unary":
                // is unary => is of the NOT kind => boolean (validated by visit unary)
                break;
            case "Binary":
                if (!childLeft.get("op").equals("AND") && !childLeft.get("op").equals("LESSTHAN")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")), node.getKind() + "  operator left operand is not a boolean."));
                    return false;
                }
                break;
            default:
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")), node.getKind() + "  operator left operand is not a boolean."));
                return false;
        }

        JmmNode childRight = node.getChildren().get(1);
        switch (childRight.getKind()) {
            case "Literal":
                Symbol childRightVar = method.getVar(childRight.get("name"));
                if ((childRightVar == null && !childLeft.get("type").equals("bool")) ||
                        (childRightVar != null && !childRightVar.getType().getName().equals("boolean"))) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")), node.getKind() + "  operator right operand is not a boolean."));
                    return false;
                }
                break;
            case "Unary":
                // is unary => is of the NOT kind => boolean (validated by visit unary)
                break;
            case "Binary":
                if (!childRight.get("op").equals("AND") && !childRight.get("op").equals("LESSTHAN")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")), node.getKind() + "  operator right operand is not a boolean."));
                    return false;
                }
                break;
            default:
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")), node.getKind() + "  operator right operand is not a boolean."));
                return false;
        }
        return true;
    }

    private boolean validateIndex(JmmNode ltNode, List<Report> reports) {
        if (ltNode.getNumChildren() != 2) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(ltNode.get("line")), "Less than operator needs 2 operands."));
            return false;
        }

        return true;
    }
}
