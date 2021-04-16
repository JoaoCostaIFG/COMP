import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.Integer.parseInt;

public class BodyVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    private final MySymbolTable symbolTable;
    private final Method method;
    private final Set<String> assignedVariables;
    private static final Type everythingType = new Type("", false);

    public BodyVisitor(MySymbolTable symbolTable, Method method) {
        super();
        this.symbolTable = symbolTable;
        this.method = method;
        this.assignedVariables = new HashSet<>();
        for (Symbol s : method.getParameters()) { // method parameters are already initialised
            assignedVariables.add(s.getName());
        }

        addVisit("New", this::visitNew);
        addVisit("Assign", this::visitAssign);
        addVisit("Unary", this::visitUnary);
        addVisit("Binary", this::visitBinary);
        addVisit("Cond", this::visitCond);
    }

    private boolean validateBooleanOp(JmmNode node, List<Report> reports) {
        JmmNode childLeft = node.getChildren().get(0);
        if (!nodeIsOfType(childLeft, "boolean", reports)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")),
                    node.getKind() + "  operator left operand is not a boolean."));
            return false;
        }

        JmmNode childRight = node.getChildren().get(1);
        if (!nodeIsOfType(childRight, "boolean", reports)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")),
                    node.getKind() + "  operator right operand is not a boolean."));
            return false;
        }

        return true;
    }

    private boolean validateArithmeticOp(JmmNode node, List<Report> reports) {
        JmmNode childLeft = node.getChildren().get(0);
        if (!nodeIsOfType(childLeft, "int", reports)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(childLeft.get("line")),
                    node.getKind() + "  operator's left operand is not a integer."));
            return false;
        }

        JmmNode childRight = node.getChildren().get(1);
        if (!nodeIsOfType(childRight, "int", reports)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(childRight.get("line")),
                    node.getKind() + "  operator's right operand is not a integer."));
            return false;
        }

        return true;
    }

    private boolean validateIndexOp(JmmNode indNode, List<Report> reports) {
        JmmNode childLeft = indNode.getChildren().get(0);
        if (!this.nodeIsOfType(childLeft, "int", true, reports)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(childLeft.get("line")),
                    "Index operator can only be used in arrays."));
            return false;
        }

        JmmNode childRight = indNode.getChildren().get(1);
        if (!this.nodeIsOfType(childRight, "int", reports)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(childRight.get("line")),
                    "Index operator's index is not an integer."));
            return false;
        }

        return true;
    }

    private boolean validateDotOp(JmmNode dotNode, List<Report> reports) {
        JmmNode childLeft = dotNode.getChildren().get(0);
        JmmNode childRight = dotNode.getChildren().get(1);

        if (childRight.getKind().equals("Len")) {
            if (!this.nodeIsOfType(childLeft, "int", true, reports)) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(childRight.get("line")),
                        "Length is a property of arrays."));
                return false;
            }
        } else { // func call
            Type leftType = this.getNodeType(childLeft, reports);
            if (leftType.getName().equals("int") || leftType.getName().equals("boolean")) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(dotNode.get("line")),
                        "Calling method in object that isn't callable."));
                return false;
            }

            if (leftType.getName().equals("this")) {
                // check if given method exists in class/super class
                Type t = getMethodCallType(dotNode, reports);
                if (t == null && symbolTable.getSuper() == null) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(childRight.get("line")),
                            "Method isn't part of the class/super class: " + childRight.get("methodName")));
                    return false;
                }
            }

            // - se for identifier, verificar se e var.
            //         - se for var, ver se esta instanciada
            //         - se nao, e uma chamada de funcao static e temos de ver se esta nos imports

            // TODO continue here
            //if (childLeft.getKind().equals("Literal")) {
            //    // TODO this only handles static methods
            //    if (!symbolTable.hasImport(childLeft.get("name"))) {
            //        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(childRight.get("line")),
            //                "Object " + childLeft.get("name") + " is unknown."));
            //        return false;
            //    }
            //}
        }

        return true;
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
                return this.validateDotOp(node, reports);
        }

        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1,
                "Unknown operation: " + node.getKind() + "."));
        return false;
    }

    private Boolean visitUnary(JmmNode node, List<Report> reports) {
        // This is always the NOT operator
        JmmNode child = node.getChildren().get(0);
        if (!this.nodeIsOfType(child, "boolean", reports)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(child.get("line")),
                    "The NOT operator can only be applied to boolean values."));
            return false;
        }
        return true;
    }

    private Boolean visitNew(JmmNode node, List<Report> reports) {
        if (node.get("type").equals("array")) {
            JmmNode arrIndexNode = node.getChildren().get(0);
            if (!this.nodeIsOfType(arrIndexNode, "int", reports)) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(arrIndexNode.get("line")),
                        "The size of an array has to be an integer."));
                return false;
            }
        }
        // else {
        // TODO can we only instantiate our own class?
        // String instName = node.get("name");
        // if (!this.symbolTable.getClassName().equals(instName)) {
        //     reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")),
        //             "Unknown class to instantiate."));
        //     return false;
        // }
        // }

        return true;
    }

    private boolean visitCond(JmmNode node, List<Report> reports) {
        if (!nodeIsOfType(node, "boolean", reports)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")),
                    "Condition is not boolean."));
            return false;
        }
        return true;
    }

    private Boolean visitAssign(JmmNode node, List<Report> reports) {
        JmmNode varNode = node.getChildren().get(0);
        String varName = varNode.get("name");
        Symbol var = this.getVar(varNode, reports, false);
        if (var == null) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(varNode.get("line")),
                    "Assignment to undeclared variable: " + varName + "."));
            return false;
        }
        Type varType = var.getType();

        JmmNode contentNode = node.getChildren().get(1);
        Type contentType = this.getNodeType(contentNode, reports);
        if (contentType == null) {
            if (contentNode.getKind().equals("Literal")) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(varNode.get("line")),
                        "Undefined variable: " + contentNode.get("name") + "."));
            }
            return false;
        }

        if (contentType != BodyVisitor.everythingType &&
                (!varType.getName().equals(contentType.getName()) ||
                varType.isArray() != contentType.isArray())) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(varNode.get("line")),
                    "Assignment variable and content have different types: " + varName + "."));
            return false;
        }

        this.assignedVariables.add(varName);
        return true;
    }

    private Symbol getVar(JmmNode varNode, List<Report> reports, boolean checkDeclared) {
        String varName = varNode.get("name");

        // check for method
        Symbol s = method.getVar(varName);
        if (s == null)  // check class scope
            s = this.symbolTable.getField(varName);
        else if (checkDeclared) {
            if (!this.assignedVariables.contains(varName)) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(varNode.get("line")),
                        "Variable used before being assigned a value: " + varName + "."));
            }
        }

        return s;
    }

    private Symbol getVar(JmmNode varNode, List<Report> reports) {
        return this.getVar(varNode, reports, true);
    }

    public Type getMethodCallType(JmmNode node, List<Report> reports) {
        JmmNode methodNode = node.getChildren().get(1);
        List<JmmNode> children = methodNode.getChildren();
        List<Type> methodParams = new ArrayList<>();
        for (int i = 1; i < methodNode.getNumChildren(); ++i) {
            JmmNode paramNode = children.get(i);
            methodParams.add(this.getNodeType(paramNode, reports));
        }

        Method foundMethod = this.symbolTable.getMethodByCall(methodNode.get("methodName"), methodParams);
        if (foundMethod == null)
            return null;
        return foundMethod.getReturnType();
    }

    private Type getDotNodeType(JmmNode node, List<Report> reports) {
        JmmNode childLeft = node.getChildren().get(0),
                childRight = node.getChildren().get(1);
        if (childRight.getKind().equals("Len")) {
            // Left child -> int[]
            // Right child -> Len
            // length can only be called on arrays
            return new Type("int", false);
        } else if (childRight.getKind().equals("FuncCall")) {
            // Right Child -> FuncCall
            if (childLeft.getKind().equals("Literal") && childLeft.get("type").equals("this")) {
                Type retType = getMethodCallType(node, reports);
                if (retType == null)
                    return BodyVisitor.everythingType;
                else
                    return retType;
            } else {
                return BodyVisitor.everythingType;
            }
        } else {
            return null;
        }
    }

    private Type getBinaryNodeType(JmmNode node, List<Report> reports) {
        String op = node.get("op");
        return switch (op) {
            case "AND", "LESSTHAN" -> new Type("boolean", false);
            case "ADD", "SUB", "MULT", "DIV", "INDEX" -> new Type("int", false);
            case "DOT" -> getDotNodeType(node, reports);
            default -> null;
        };
    }

    private Type getLiteralNodeType(JmmNode node, List<Report> reports) {
        if (node.get("type").equals("identifier")) {
            Symbol s = this.getVar(node, reports);
            if (s == null)
                return null;
            return s.getType();
        } else { // Is type - boolean, int or array
            return new Type(node.get("type"), false);
        }
    }

    private Type getNewNodeType(JmmNode node) {
        if (node.get("type").equals("array")) { // int array
            return new Type("int", true);
        } else { // class instance
            return new Type(node.get("name"), false);
        }
    }

    public Type getNodeType(JmmNode node, List<Report> reports) {
        return switch (node.getKind()) {
            case "Binary" -> this.getBinaryNodeType(node, reports);
            case "Literal" -> this.getLiteralNodeType(node, reports);
            case "Unary" -> new Type("boolean", false);
            case "New" -> getNewNodeType(node);
            default -> null;
        };
    }

    public boolean nodeIsOfType(JmmNode node, String type, boolean isArray, List<Report> reports) {
        Type t = getNodeType(node, reports);
        return t != null &&
                (t == BodyVisitor.everythingType || t.getName().equals(type) && isArray == t.isArray());
    }

    public boolean nodeIsOfType(JmmNode node, String type, List<Report> reports) {
        return nodeIsOfType(node, type, false, reports);
    }
}
