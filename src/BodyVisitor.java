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
        addVisit("New", this::visitNew);
        addVisit("Assign", this::visitAssign);
    }

    private Boolean visitBinary(JmmNode node, List<Report> reports) {
        String op = node.get("op");
        switch (op) {
            case "AND":
            case "LESSTHAN":
                return this.validateBooleanOp(node, reports);
            case "ADD":
            case "SUB":
            case "MULT":
            case "DIV":
                return this.validateArithmeticOp(node, reports);
            case "INDEX":
                return this.validateIndexOp(node, reports);
            case "DOT":
                break;
        }

        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")),
                "Unknown operation."));
        return false;
    }

    private Boolean visitUnary(JmmNode node, List<Report> reports) {
        // This is always the NOT operator
        JmmNode child = node.getChildren().get(0);
        if (!this.nodeIsOfType(child, "boolean")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(child.get("line")),
                    "The NOT operator can only be applied to boolean values."));
            return false;
        }
        return true;
    }

    private Boolean visitNew(JmmNode node, List<Report> reports) {
        if (node.get("type").equals("array")) {
            JmmNode arrIndexNode = node.getChildren().get(0);
            if (!this.nodeIsOfType(arrIndexNode, "int")) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(arrIndexNode.get("line")),
                        "The size of an array has to be an integer."));
                return false;
            }
        } else {
            // TODO ??????
            String instName = node.get("name");
            if (!this.symbolTable.getClassName().equals(instName)) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")),
                        "Unknown class to instantiate."));
                return false;
            }
        }

        return true;
    }

    private Boolean visitAssign(JmmNode node, List<Report> reports) {
        return true;
    }

    public Type getMethodCallType(JmmNode node) {
        JmmNode methodNode = node.getChildren().get(1);
        List<JmmNode> children = methodNode.getChildren();
        List<Type> methodParams = new ArrayList<>();
        for (int i = 1; i < methodNode.getNumChildren(); ++i) {
            JmmNode paramNode = children.get(i);
            switch (paramNode.getKind()) {
                case "Binary":
                    switch (paramNode.get("op")) {
                        case "LESSTHAN":
                        case "AND":
                            methodParams.add(new Type("boolean", false));
                            break;
                        case "SUB":
                        case "INDEX":
                        case "DIV":
                        case "MULT":
                        case "ADD":
                            methodParams.add(new Type("int", false));
                            break;
                        case "DOT":
                            methodParams.add(getDotNodeType(paramNode));
                            break;
                    } // VOU DAR PUSH DECLARO AQUI QUE VOU DAR PUSH
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
                                return null;
                            methodParams.add(var.getType());
                            break;
                        default:
                            return null;
                    }
                    break;
                default:
                    return null;
            }
        }

        Method foundMethod = this.symbolTable.getMethodByCall(methodNode.get("methodName"), methodParams);
        if (foundMethod == null)
            return null;
        return new Type(foundMethod.getReturnType().getName(), false);
    }

    public boolean methodCallIsOfType(JmmNode node, String type) {
        JmmNode container = node.getChildren().get(0);
        // 'outside' methods, are assumed to have the correct type
        if (!container.get("type").equals("this")) {
            // TODO verificar se esta nos imports
            return true;
        }

        Type t = getMethodCallType(node);
        // TODO check for super when t == null
        return t != null && t.getName().equals(type);
    }

    private Type getDotNodeType(JmmNode node) {
        JmmNode childLeft = node.getChildren().get(0),
                childRight = node.getChildren().get(1);
        if (childRight.getKind().equals("Len")) {
            // Left child -> int[]
            // Right child -> Len
            // length can only be called on arrays
            if (!this.nodeIsOfType(childLeft, "int", true))
                return null;
            return new Type("int", true);
        } else if (childRight.getKind().equals("FuncCall")) {
            // Right Child -> FuncCall
            return getMethodCallType(node);
        } else {
            return null;
        }
    }

    private Type getBinaryNodeType(JmmNode node) {
        String op = node.get("op");
        return switch (op) {
            case "AND", "LESSTHAN" -> new Type("boolean", false);
            case "ADD", "SUB", "MULT", "DIV" -> new Type("int", false);
            case "DOT" -> getDotNodeType(node);
            default -> null;
        };
    }

    private Type getLiteralNodeType(JmmNode node) {
        if (node.get("type").equals("identifier")) {
            String nodeName = node.get("name");
            Symbol s;
            // check for method
            s = method.getVar(nodeName);
            if (s == null)  // check class scope
                s = this.symbolTable.getField(nodeName);
            if(s == null)
                return null;
            return s.getType();
        } else { // Is type - boolean, int or array
            return new Type(node.get("type"), false);
        }
    }

    public Type getNodeType(JmmNode node) {
        return switch (node.getKind()) {
            case "Binary" -> getBinaryNodeType(node);
            case "Literal" -> getLiteralNodeType(node);
            case "Unary" -> new Type("boolean", false);
            default -> null;
        };
    }

    public boolean nodeIsOfType(JmmNode node, String type, boolean isArray) {
        Type t = getNodeType(node);
        return t != null && t.getName().equals(type) && isArray == t.isArray();
    }

    public boolean nodeIsOfType(JmmNode node, String type) {
        return nodeIsOfType(node, type, false);
    }

    private boolean validateBooleanOp(JmmNode node, List<Report> reports) {
        if (node.getNumChildren() != 2) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")),
                    node.getKind() + " operator needs 2 operands."));
            return false;
        }

        JmmNode childLeft = node.getChildren().get(0);
        if (!nodeIsOfType(childLeft, "boolean")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")),
                    node.getKind() + "  operator left operand is not a boolean."));
            return false;
        }

        JmmNode childRight = node.getChildren().get(1);
        if (!nodeIsOfType(childRight, "boolean")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")),
                    node.getKind() + "  operator right operand is not a boolean."));
            return false;
        }

        return true;
    }

    private boolean validateArithmeticOp(JmmNode node, List<Report> reports) {
        if (node.getNumChildren() != 2) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")),
                    node.getKind() + " operator needs 2 operands."));
            return false;
        }

        JmmNode childLeft = node.getChildren().get(0);
        if (!nodeIsOfType(childLeft, "int")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")),
                    node.getKind() + "  operator left operand is not a integer."));
            return false;
        }

        JmmNode childRight = node.getChildren().get(1);
        if (!nodeIsOfType(childRight, "int")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")),
                    node.getKind() + "  operator right operand is not a integer."));
            return false;
        }

        return true;
    }

    private boolean validateIndexOp(JmmNode indNode, List<Report> reports) {
        if (indNode.getNumChildren() != 2) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(indNode.get("line")),
                    "Index operator needs 2 operands."));
            return false;
        }

        JmmNode childLeft = indNode.getChildren().get(0);
        if (this.nodeIsOfType(childLeft, "int", true)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(childLeft.get("line")),
                    "Index operator can only be used in arrays."));
            return false;
        }

        JmmNode childRight = indNode.getChildren().get(1);
        if (!this.nodeIsOfType(childRight, "int")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(childRight.get("line")),
                    "Index operator's index is not an integer."));
            return false;
        }

        return true;
    }
}
