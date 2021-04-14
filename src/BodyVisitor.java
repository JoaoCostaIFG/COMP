import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.List;

import static java.lang.Integer.parseInt;

public class BodyVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    private final MySymbolTable symbolTable;

    public BodyVisitor(MySymbolTable symbolTable) {
        super();
        this.symbolTable = symbolTable;
        addVisit("Binary", this::visitBinary);
        addVisit("Unary", this::visitUnary);
    }

    private Boolean visitBinary(JmmNode node, List<Report> reports) {
        String op = node.get("op");
        switch (op) {
            case "AND":
                return this.validateAnd(node, reports);
            case "LESSTHAN":
                break;
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
            default:
                // this never happens
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")), "Unknown operation."));
                return false;
        }
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

    private boolean validateAnd(JmmNode andNode, List<Report> reports) {
        if (andNode.getNumChildren() != 2) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(andNode.get("line")), "And operator needs 2 operands."));
            return false;
        }

        JmmNode childLeft = andNode.getChildren().get(0);
        switch (childLeft.getKind()) {
            case "Literal":
                if (!childLeft.get("type").equals("bool")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(andNode.get("line")), "And operator left operand is not a boolean."));
                    return false;
                }
                break;
            case "Unary":
                // is unary => is of the NOT kind => boolean (validated by visit unary)
                break;
            case "Binary":
                if (!childLeft.get("op").equals("AND") && !childLeft.get("op").equals("LESSTHAN")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(andNode.get("line")), "And operator left operand is not a boolean."));
                    return false;
                }
                break;
            default:
                break;
        }

        JmmNode childRight = andNode.getChildren().get(1);
        switch (childRight.getKind()) {
            case "Literal":
                if (!childRight.get("type").equals("bool")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(andNode.get("line")), "And operator right operand is not a boolean."));
                    return false;
                }
                break;
            case "Unary":
                // is unary => is of the NOT kind => boolean (validated by visit unary)
                break;
            case "Binary":
                if (!childRight.get("op").equals("AND") && !childRight.get("op").equals("LESSTHAN")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(andNode.get("line")), "And operator right operand is not a boolean."));
                    return false;
                }
                break;
            default:
                break;
        }

        return true;
    }
}
