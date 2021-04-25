import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.Integer.parseInt;

public class BodyVisitor extends PostorderJmmVisitor<List<Report>, Boolean> {
    private final MySymbolTable symbolTable;
    private final Method method;
    private final String methodName;
    private final Set<String> assignedVariables;
    private boolean isMain = false;

    private static final Type everythingType = new Type("", true);
    private static final Symbol everythingSymbol = new Symbol(everythingType, "");

    public BodyVisitor(MySymbolTable symbolTable, Method method, String methodName) {
        super();
        this.symbolTable = symbolTable;
        this.method = method;
        this.methodName = methodName;
        this.assignedVariables = new HashSet<>();
        for (Symbol s : method.getParameters()) { // method parameters are already initialised
            assignedVariables.add(s.getName());
        }

        addVisit("MethodDeclaration", this::visitMethodRoot);
        addVisit("New", this::visitNew);
        addVisit("Assign", this::visitAssign);
        addVisit("Binary", this::visitBinary);
        addVisit("Unary", this::visitUnary);
        addVisit("Literal", this::visitLiteral);
        addVisit("Cond", this::visitCond);
        addVisit("Return", this::visitReturn);
    }

    private Boolean visitMethodRoot(JmmNode jmmNode, List<Report> reports) {
        for (JmmNode child : jmmNode.getChildren()) {
            if (child.getKind().equals("MainParameter")) {
                this.isMain = true;
            }
        }
        return true;
    }

    private boolean validateBooleanOp(JmmNode node, List<Report> reports) {
        JmmNode childLeft = node.getChildren().get(0);
        if (!nodeIsOfType(childLeft, "boolean", reports)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    parseInt(node.get("line")), parseInt(node.get("col")),
                    node.getKind() + "  operator left operand is not a boolean."));
            return false;
        }

        JmmNode childRight = node.getChildren().get(1);
        if (!nodeIsOfType(childRight, "boolean", reports)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    parseInt(node.get("line")), parseInt(node.get("col")),
                    node.getKind() + "  operator right operand is not a boolean."));
            return false;
        }

        return true;
    }

    private boolean validateArithmeticOp(JmmNode node, List<Report> reports) {
        JmmNode childLeft = node.getChildren().get(0);
        if (!nodeIsOfType(childLeft, "int", reports)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    parseInt(childLeft.get("line")), parseInt(childLeft.get("col")),
                    node.getKind() + " operator's left operand is not a integer."));
            return false;
        }

        JmmNode childRight = node.getChildren().get(1);
        if (!nodeIsOfType(childRight, "int", reports)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    parseInt(childRight.get("line")), parseInt(childRight.get("col")),
                    node.getKind() + " operator's right operand is not a integer."));
            return false;
        }

        return true;
    }

    private boolean validateIndexOp(JmmNode indNode, List<Report> reports) {
        JmmNode childLeft = indNode.getChildren().get(0);
        Type type = this.getNodeType(childLeft, reports);
        if (!type.isArray()) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    parseInt(childLeft.get("line")), parseInt(childLeft.get("col")),
                    "Index operator can only be used in arrays."));
            return false;
        }

        JmmNode childRight = indNode.getChildren().get(1);
        if (!this.nodeIsOfType(childRight, "int", reports)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    parseInt(childRight.get("line")), parseInt(childRight.get("col")),
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
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                        parseInt(childRight.get("line")), parseInt(childRight.get("col")),
                        "Length is a property of arrays."));
                return false;
            }
        } else { // func call
            Type leftType = this.getNodeType(childLeft, reports);
            if (leftType == null) { // is something unknown => if is import
                return symbolTable.hasImport(childLeft.get("name"));
            } else if (leftType.getName().equals("int") || leftType.getName().equals("boolean")) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                        parseInt(dotNode.get("line")), parseInt(dotNode.get("col")),
                        "Calling method in object that isn't callable."));
                return false;
            }

            // is instance of own class
            if (leftType.getName().equals("this") || leftType.getName().equals(this.symbolTable.getClassName())) {
                if (this.isMain && leftType.getName().equals("this")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                            parseInt(childRight.get("line")), parseInt(childRight.get("col")),
                            "This cannot be referenced from a static context."));
                    return false;
                }
                // check if given method exists in class/super class
                Type t = getMethodCallType(dotNode, reports);
                if (t == null && symbolTable.getSuper() == null) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                            parseInt(childRight.get("line")), parseInt(childRight.get("col")),
                            "Method isn't part of the class/super class: " + childRight.get("methodName")));
                    return false;
                }
            }
        }

        return true;
    }

    private Boolean visitReturn(JmmNode node, List<Report> reports) {
        JmmNode child = node.getChildren().get(0);
        Type retType = this.symbolTable.getReturnType(this.methodName);
        if (!this.nodeIsOfType(child, retType, reports)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    parseInt(child.get("line")), parseInt(child.get("col")),
                    "Method's return statement doesn't have the correct type: " +
                            retType.getName() + (retType.isArray() ? "[]" : "")));
            return false;
        }
        return true;
    }

    private boolean checkNotAStatement(JmmNode node, List<Report> reports) {
        JmmNode parent = node.getParent();
        String kind = parent.getKind();
        // if the parent is the method body or if body or while body, we are not a statement
        // (if we are AND, ADD, SUB, MULT, DIV, LESSTHAN, INDEX, or NOT)
        if (kind.equals("MethodBody") || kind.equals("IfBody") || kind.equals("ElseBody") || kind.equals("Body")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    parseInt(node.get("line")), parseInt(node.get("col")),
                    "Not a statement."));
            return false;
        }
        return true;
    }

    private Boolean visitBinary(JmmNode node, List<Report> reports) {
        String op = node.get("op");
        switch (op) {
            case "AND":
                if (!this.checkNotAStatement(node, reports))
                    return false;
                return this.validateBooleanOp(node, reports);
            case "ADD":
            case "SUB":
            case "MULT":
            case "DIV":
            case "LESSTHAN":
                if (!this.checkNotAStatement(node, reports))
                    return false;
                return this.validateArithmeticOp(node, reports);
            case "INDEX":
                if (!this.checkNotAStatement(node, reports))
                    return false;
                return this.validateIndexOp(node, reports);
            case "DOT":
                return this.validateDotOp(node, reports);
        }

        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")),
                "Unknown operation: " + node.getKind() + "."));
        return false;
    }

    private Boolean visitLiteral(JmmNode node, List<Report> reports) {
        return this.checkNotAStatement(node, reports);
    }

    private Boolean visitUnary(JmmNode node, List<Report> reports) {
        if (!this.checkNotAStatement(node, reports))
            return false;

        // This is always the NOT operator
        JmmNode child = node.getChildren().get(0);
        if (!this.nodeIsOfType(child, "boolean", reports)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    parseInt(child.get("line")), parseInt(child.get("col")),
                    "The NOT operator can only be applied to boolean values."));
            return false;
        }
        return true;
    }

    private Boolean visitNew(JmmNode node, List<Report> reports) {
        if (node.get("type").equals("array")) {
            JmmNode arrIndexNode = node.getChildren().get(0);
            if (!this.nodeIsOfType(arrIndexNode, "int", reports)) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                        parseInt(arrIndexNode.get("line")), parseInt(arrIndexNode.get("col")),
                        "The size of an array has to be an integer."));
                return false;
            }
        }

        return true;
    }

    private boolean visitCond(JmmNode node, List<Report> reports) {
        if (!nodeIsOfType(node.getChildren().get(0), "boolean", reports)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    parseInt(node.get("line")), parseInt(node.get("col")),
                    "Condition is not boolean."));
            return false;
        }
        return true;
    }

    private Boolean visitAssign(JmmNode node, List<Report> reports) {
        JmmNode varNode = node.getChildren().get(0);
        String varName = varNode.get("name");
        // get var and check that it is declared
        Symbol var = this.getVar(varNode, reports, false);
        if (var == null) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    parseInt(varNode.get("line")), parseInt(varNode.get("col")),
                    "Assignment to undeclared variable: " + varName + "."));
            return false;
        }
        Type varType = var.getType();

        // array accesses can only be performed on arrays
        boolean varIsArrayAccess = varNode.get("isArrayAccess").equals("yes");
        if (varIsArrayAccess && !varType.isArray()) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    parseInt(varNode.get("line")), parseInt(varNode.get("col")),
                    "Array access on non-array type variable: " + varName + "."));
            return false;
        }

        JmmNode contentNode = node.getChildren().get(1);
        Type contentType = this.getNodeType(contentNode, reports);
        if (contentType == null) {
            return false;
        }

        if (contentType != BodyVisitor.everythingType) {  // not everything type
            if (varIsArrayAccess) {
                // not an assignment to array index
                if (!varType.getName().equals(contentType.getName()) || contentType.isArray()) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                            parseInt(varNode.get("line")), parseInt(varNode.get("col")),
                            "Assignment variable and content have different types: " + varName + "."));
                    return false;
                }
            } else {  // not a normal var assignment
                if (!varType.getName().equals(contentType.getName()) ||
                        varType.isArray() != contentType.isArray()) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                            parseInt(varNode.get("line")), parseInt(varNode.get("col")),
                            "Assignment variable and content have different types: " + varName + "."));
                    return false;
                }
            }
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
        else if (checkDeclared) { // only check for declarations of vars in our scope
            if (!this.assignedVariables.contains(varName)) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                        parseInt(varNode.get("line")), parseInt(varNode.get("col")),
                        "Variable used before being assigned a value: " + varName + "."));
            }
        }

        // variable doesn't exist
        if (s == null) {
            // check for imports with that name
            if (this.symbolTable.hasImport(varName))
                return null;

            // unknown => return something to appease the masses
            if (checkDeclared) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                        parseInt(varNode.get("line")), parseInt(varNode.get("col")),
                        "Variable is undeclared: " + varName + "."));
            }
            return BodyVisitor.everythingSymbol;
        }
        return s;
    }

    private Symbol getVar(JmmNode varNode, List<Report> reports) {
        return this.getVar(varNode, reports, true);
    }

    public Type getMethodCallType(JmmNode node, List<Report> reports) {
        JmmNode methodNode = node.getChildren().get(1);
        List<Type> methodParams = new ArrayList<>();

        // save all args types (if any)
        if (methodNode.getNumChildren() > 0) {
            JmmNode argsNode = methodNode.getChildren().get(0);
            List<JmmNode> children = argsNode.getChildren();
            for (JmmNode paramNode : children)
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
        } else { // FuncCall
            // Right Child -> FuncCall
            Type leftType = this.getNodeType(childLeft, reports);
            if (childLeft.getKind().equals("Literal") &&
                    (childLeft.get("type").equals("this") ||
                            (leftType != null && leftType.getName().equals(this.symbolTable.getClassName())))) {
                Type retType = getMethodCallType(node, reports);
                if (retType == null)
                    return BodyVisitor.everythingType;
                else
                    return retType;
            } else {
                return BodyVisitor.everythingType;
            }
        }
    }

    private Type getBinaryNodeType(JmmNode node, List<Report> reports) {
        String op = node.get("op");
        switch (op) {
            case "AND":
            case "LESSTHAN":
                return new Type("boolean", false);
            case "ADD":
            case "SUB":
            case "MULT":
            case "DIV":
            case "INDEX":
                return new Type("int", false);
            case "DOT":
                return this.getDotNodeType(node, reports);
            default:
                return null;
        }
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
        switch (node.getKind()) {
            case "Binary":
                return this.getBinaryNodeType(node, reports);
            case "Literal":
                return this.getLiteralNodeType(node, reports);
            case "Unary":
                return new Type("boolean", false);
            case "New":
                return this.getNewNodeType(node);
            default:
                return null;
        }
    }

    public boolean nodeIsOfType(JmmNode node, String type, boolean isArray, List<Report> reports) {
        Type t = getNodeType(node, reports);
        return t != null &&
                (t == BodyVisitor.everythingType || t.getName().equals(type) && isArray == t.isArray());
    }

    public boolean nodeIsOfType(JmmNode node, String type, List<Report> reports) {
        return this.nodeIsOfType(node, type, false, reports);
    }

    public boolean nodeIsOfType(JmmNode node, Type type, List<Report> reports) {
        return this.nodeIsOfType(node, type.getName(), type.isArray(), reports);
    }
}
