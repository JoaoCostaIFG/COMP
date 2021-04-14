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
                return this.validateIndex(node, reports);
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
                if (nodeIsOfType(child, "boolean")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")),
                            "Not operator operand is not a boolean."));
                    return false;
                }
                break;
            case "Unary":
                // is unary => is of the NOT kind => boolean (validated by visit unary)
                break;
            case "Binary":
                if (!child.get("op").equals("AND") && !child.get("op").equals("LESSTHAN")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")),
                            "Not operator operand is not a boolean."));
                    return false;
                }
                break;
            default:
                break;
        }

        return true;
    }

    public boolean nodeIsOfType(JmmNode node, String type, boolean isArray) {
        if (node.get("type").equals("identifier")) {
            Symbol s = method.getVar(node.get("name"));
            return s != null && s.getType().getName().equals(type) &&
                    s.getType().isArray() == isArray;
        } else {
            return node.get("type").equals(type);
        }
    }

    public boolean nodeIsOfType(JmmNode node, String type) {
        return nodeIsOfType(node, type, false);
    }

    public boolean methodCallIsOfType(JmmNode node, String type) {
        JmmNode container = node.getChildren().get(0);
        // 'outside' methods, are assumed to have the correct type
        if (container.get("type").equals("this")) {
            return true;
        }

        JmmNode methodNode = node.getChildren().get(1);
        List<JmmNode> children = methodNode.getChildren();
        List<Type> methodParams = new ArrayList<>();
        for (int i = 1; i < methodNode.getNumChildren(); ++i) {
            JmmNode paramNode = children.get(i);
            switch (paramNode.getKind()) {
                case "Binary":
                    switch (paramNode.get("op")) {
                        case "AND":
                        case "LESSTHAN":
                            methodParams.add(new Type("boolean", false));
                            break;
                        case "ADD":
                        case "SUB":
                        case "MULT":
                        case "DIV":
                        case "INDEX":
                            methodParams.add(new Type("int", false));
                            break;
                        case "DOT":
                            // TODO ?????????????
                            break;
                    }
                    break;
                case "Unary":
                    methodParams.add(new Type("boolean", false));
                    break;
                case "Literal":
                    switch (paramNode.get("type")) {
                        case "boolean":
                            methodParams.add(new Type("boolean", false));
                            break;
                        case "int":
                            methodParams.add(new Type("int", false));
                            break;
                        case "identifier":
                            Symbol var = this.method.getVar(paramNode.get("name"));
                            if (var == null)
                                return false;
                            methodParams.add(var.getType());
                            break;
                        default:
                            return false;
                    }
                    break;
                default:
                    return false;
            }
        }

        Method foundMethod = this.symbolTable.getMethodByCall(methodNode.get("methodName"), methodParams);
        if (foundMethod == null)
            return false;
        return foundMethod.getReturnType().getName().equals(type);
    }

    private boolean validateBoolean(JmmNode node, List<Report> reports) {
        if (node.getNumChildren() != 2) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")),
                    node.getKind() + " operator needs 2 operands."));
            return false;
        }

        JmmNode childLeft = node.getChildren().get(0);
        switch (childLeft.getKind()) {
            case "Literal":
                if (nodeIsOfType(childLeft, "boolean")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")),
                            node.getKind() + "  operator left operand is not a boolean."));
                    return false;
                }
                break;
            case "Unary":
                // is unary => is of the NOT kind => boolean (validated by visit unary)
                break;
            case "Binary":
                if (!childLeft.get("op").equals("AND") && !childLeft.get("op").equals("LESSTHAN")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")),
                            node.getKind() + "  operator left operand is not a boolean."));
                    return false;
                }
                break;
            default:
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")),
                        node.getKind() + "  operator left operand is not a boolean."));
                return false;
        }

        JmmNode childRight = node.getChildren().get(1);
        switch (childRight.getKind()) {
            case "Literal":
                if (nodeIsOfType(childRight, "boolean")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")),
                            node.getKind() + "  operator right operand is not a boolean."));
                    return false;
                }
                break;
            case "Unary":
                // is unary => is of the NOT kind => boolean (validated by visit unary)
                break;
            case "Binary":
                if (!childRight.get("op").equals("AND") && !childRight.get("op").equals("LESSTHAN")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")),
                            node.getKind() + "  operator right operand is not a boolean."));
                    return false;
                }
                break;
            default:
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")),
                        node.getKind() + "  operator right operand is not a boolean."));
                return false;
        }
        return true;
    }

    private boolean validateIndex(JmmNode indNode, List<Report> reports) {
        if (indNode.getNumChildren() != 2) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(indNode.get("line")),
                    "Index operator needs 2 operands."));
            return false;
        }

        JmmNode childLeft = indNode.getChildren().get(0);
        if (!(childLeft.getKind().equals("Literal") && this.nodeIsOfType(childLeft, "int", true))) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(indNode.get("line")),
                    "Index operator can only be used in arrays."));
            return false;
        }

        JmmNode childRight = indNode.getChildren().get(1);
        switch (childRight.getKind()) {
            case "Literal":
                if (nodeIsOfType(childRight, "int")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(indNode.get("line")),
                            indNode.getKind() + "  operator's index is not an integer."));
                    return false;
                }
                break;
            case "Binary":
                String binOp = childRight.get("op");
                // these all evaluate to an integer (int)
                if (!(binOp.equals("ADD") || binOp.equals("SUB") || binOp.equals("MULT") ||
                        binOp.equals("DIV") || binOp.equals("INDEX"))) {
                    // if it is a method call, check return type
                    if (binOp.equals("DOT")) {
                        if (this.methodCallIsOfType(childRight, "int"))
                            break;
                    }

                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(indNode.get("line")),
                            indNode.getKind() + "  operator's index is not an integer."));
                    return false;
                }
                break;
            default:
                break;
        }

        return true;
    }
}
