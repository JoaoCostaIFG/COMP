import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;

import java.util.ArrayList;
import java.util.List;

public class OllirEmitter extends PreorderJmmVisitor<Boolean, String> {
    private final MySymbolTable symbolTable;
    private final StringBuilder ollirCode;
    private int labelCount;
    private Integer auxCount;
    private List<Symbol> localVars, parameters;

    public OllirEmitter(MySymbolTable symbolTable) {
        super();
        this.symbolTable = symbolTable;
        this.ollirCode = new StringBuilder();
        this.auxCount = 0;
        this.labelCount = 0;
        this.localVars = new ArrayList<>();
        this.parameters = new ArrayList<>();
        this.addVisit("Program", this::visitRoot);
    }

    public String getOllirCode() {
        return this.ollirCode.toString();
    }

    private String getNextAuxVar() {
        String auxVarName = "aux" + this.auxCount.toString();
        ++this.auxCount;
        return auxVarName;
    }

    private String getNextLabel(String pre) {
        return pre + (this.labelCount++);
    }

    private void loadMethod(Method method) {
        this.localVars = method.getLocalVars();
        this.parameters = method.getParameters();
    }

    private String primitiveType(Type type) {
        if (type.getName().equals("int"))
            return "i32";
        else if (type.getName().equals("boolean"))
            return "bool";
        else
            return null;
    }

    private String getTypeOllir(Type type) {
        String ret = ".";
        if (type.isArray())
            ret += "array.";

        String primitiveType = this.primitiveType(type);
        if (primitiveType == null)
            ret += type.getName();
        else
            ret += primitiveType;

        return ret;
    }

    private String getSymbolOllir(Symbol var) {
        return var.getName() + this.getTypeOllir(var.getType());
    }

    private String visitRoot(JmmNode node, Boolean ignored) {
        this.ollirCode.append("class ").append(this.symbolTable.getClassName()).append(" {\n");
        // TODO extends

        // class fields
        for (Symbol field : this.symbolTable.getFields()) {
            this.ollirCode.append("\t.field private ").append(this.getSymbolOllir(field)).append(";\n");
        }

        // constructor
        this.ollirCode.append("\t.construct ").append(this.symbolTable.getClassName()).append("().V {\n");
        this.ollirCode.append("\t\tinvokespecial(this, \"<init>\").V;\n");
        this.ollirCode.append("\t}\n");

        for (String methodName : this.symbolTable.getMethods()) {
            this.getMethodOllir(methodName);
        }

        this.ollirCode.append("}");
        return this.ollirCode.toString();
    }

    private void getMethodOllir(String methodId) {
        Method method = this.symbolTable.getMethod(methodId);
        this.loadMethod(method);

        String methodName = method.getName();
        String tabs = "\t";
        this.ollirCode.append(tabs).append(".method public ");
        if (methodName.equals("main"))
            this.ollirCode.append("static ");

        // parameters
        this.ollirCode.append(methodName).append("(");
        boolean first = true;
        for (Symbol param : this.symbolTable.getParameters(methodId)) {
            this.ollirCode.append(first ? "" : ", ").append(this.getSymbolOllir(param));
            first = false;
        }
        this.ollirCode.append(")").append(this.getTypeOllir(this.symbolTable.getReturnType(methodId))).append(" {\n");

        // method body
        JmmNode methodNode = method.getNode();
        JmmNode methodBodyNode = null;
        JmmNode methodRetNode = null;
        for (JmmNode n : methodNode.getChildren()) {
            if (n.getKind().equals("MethodBody"))
                methodBodyNode = n;
            else if (n.getKind().equals("Return"))
                methodRetNode = n;
        }

        if (methodBodyNode != null)
            this.getBodyOllir(tabs + "\t", methodBodyNode);

        // TODO return statement

        this.ollirCode.append(tabs).append("}\n");
    }

    private void getBodyOllir(String tabs, JmmNode node) {
        for (JmmNode n : node.getChildren()) {
            switch (n.getKind()) {
                case "New":
                case "Literal":
                case "Binary":
                case "Unary":
                case "Assign":
                    this.ollirCode.append(this.getOpOllir(tabs, n)).append(";\n");
                    break;
                case "If":
                    this.getIfOllir(tabs, n);
                    break;
                case "WhileLoop":
                    this.getWhileOllir(tabs, n);
                    break;
            }
        }
    }

    private void getWhileOllir(String tabs, JmmNode n) {
        JmmNode condNode = n.getChildren().get(0);
        JmmNode body = n.getChildren().get(1);

        String loopLabel = this.getNextLabel("Loop");
        String endLabel = this.getNextLabel("EndLoop");

        this.ollirCode.append(tabs).append(loopLabel).append(":\n");
        String condOllir = this.getOpOllir(tabs + "\t", condNode.getChildren().get(0));
        this.ollirCode.append(tabs).append("\t").append("if (").append(condOllir.trim())
                .append(") goto ").append(endLabel).append(";\n");
        this.getBodyOllir(tabs + "\t", body);
        this.ollirCode.append(tabs).append(endLabel).append(":\n");
    }

    public void getIfOllir(String tabs, JmmNode n) {
        JmmNode condNode = n.getChildren().get(0);
        JmmNode body = n.getChildren().get(1);
        JmmNode elseBody = n.getChildren().get(2);

        String condOllir = this.getOpOllir(tabs, condNode.getChildren().get(0));
        String elseLabel = this.getNextLabel("else");
        String endLabel = this.getNextLabel("endif");

        // If condition
        this.ollirCode.append(tabs).append("if (").append(condOllir.trim()).append(") goto ").append(elseLabel).append(";\n");
        // If BOdy
        this.getBodyOllir(tabs + "\t", body);
        this.ollirCode.append(tabs).append("\tgoto ").append(endLabel).append(";\n");
        // Else
        this.ollirCode.append(tabs).append(elseLabel).append(":\n");
        this.getBodyOllir(tabs + "\t", elseBody);
        this.ollirCode.append(tabs).append(endLabel).append(":").append("\n");
    }

    private String injectTempVar(String tabs, String type, String content) {
        String auxVarName = this.getNextAuxVar() + type;
        this.ollirCode.append(tabs).append(auxVarName).append(" :=")
                .append(type).append(" ").append(content).append(";\n");
        return auxVarName;
    }

    private String getIntOpOllir(String tabs, JmmNode node, String op, boolean isAux) {
        final String type = ".i32";
        List<JmmNode> children = node.getChildren();
        String childLeftOllir = this.getOpOllir(tabs, children.get(0), true);
        String childRightOllir = this.getOpOllir(tabs, children.get(1), true);
        String ret = childLeftOllir + " " + op + type + " " + childRightOllir;

        if (isAux)
            return this.injectTempVar(tabs, type, ret);
        return ret;
    }

    private String getAndOllir(String tabs, JmmNode node, boolean isAux) {
        final String type = ".bool";
        List<JmmNode> children = node.getChildren();
        String childLeftOllir = this.getOpOllir(tabs, children.get(0), true);
        String childRightOllir = this.getOpOllir(tabs, children.get(1), true);
        String ret = childLeftOllir + " &&" + type + " " + childRightOllir;

        if (isAux)
            return this.injectTempVar(tabs, type, ret);
        return ret;
    }

    private String getLessThanOllir(String tabs, JmmNode node, boolean isAux) {
        final String retType = ".bool", type = ".i32";
        List<JmmNode> children = node.getChildren();
        String childLeftOllir = this.getOpOllir(tabs, children.get(0), true);
        String childRightOllir = this.getOpOllir(tabs, children.get(1), true);
        String ret = childRightOllir + " >=" + type + " " + childLeftOllir;  // IMP operators have to be flipped

        if (isAux)
            return this.injectTempVar(tabs, retType, ret);
        return ret;
    }

    private String getDotOllir(String tabs, JmmNode node, boolean isAux) {
        List<JmmNode> children = node.getChildren();
        String ret = "";
        String type = "";

        if (children.get(1).getKind().equals("Len")) {
            type = ".i32";
            ret += "arraylength(" + this.getOpOllir(tabs, children.get(0)) + ")" + type;
        } else { // func call
            // TODO
            ret += "invokestatic(" + children.get(0).get("name") +
                    ", \"" + children.get(1).get("methodName") + "\"" + ").V";
        }

        if (isAux)
            return this.injectTempVar(tabs, type, ret);
        return ret;
    }

    private String getIndexOllir(String tabs, JmmNode node) {
        final String type = ".i32";
        List<JmmNode> children = node.getChildren();
        String childLeftOllir = this.getOpOllir(tabs, children.get(0), true);
        String childRightOllir = this.getOpOllir(tabs, children.get(1), true);

        // IMP array access have always to be stored in a temporary variable before usage
        // we are splitting the left child by "." on the left because we don't want the type to show
        String ret = childLeftOllir.split("\\.")[0] + "[" + childRightOllir + "]" + type;
        return this.injectTempVar(tabs, type, ret);
    }

    private String getBinaryOllir(String tabs, JmmNode node, boolean isAux) {
        switch (node.get("op")) {
            case "ADD":
                return this.getIntOpOllir(tabs, node, "+", isAux);
            case "SUB":
                return this.getIntOpOllir(tabs, node, "-", isAux);
            case "MULT":
                return this.getIntOpOllir(tabs, node, "*", isAux);
            case "DIV":
                return this.getIntOpOllir(tabs, node, "/", isAux);
            case "AND":
                return this.getAndOllir(tabs, node, isAux);
            case "LESSTHAN":
                return this.getLessThanOllir(tabs, node, isAux);
            case "DOT":
                return this.getDotOllir(tabs, node, isAux);
            case "INDEX":
                return this.getIndexOllir(tabs, node);
            default:
                return "";
        }
    }

    private String getUnaryOllir(String tabs, JmmNode node, boolean isAux) {
        final String type = ".bool";
        JmmNode child = node.getChildren().get(0);
        String childOllir = this.getOpOllir(tabs, child, true);
        String ret = childOllir + " !" + type + " " + childOllir;

        if (isAux)
            return this.injectTempVar(tabs, type, ret);
        return ret;
    }

    private String getVarType(String name) {
        // local variable
        for (Symbol s : this.localVars) {
            if (s.getName().equals(name)) {
                return this.getTypeOllir(s.getType());
            }
        }
        // method parameter
        for (Symbol s : this.parameters) {
            if (s.getName().equals(name)) {
                return this.getTypeOllir(s.getType());
            }
        }
        // class field
        for (Symbol s : this.symbolTable.getFields()) {
            if (s.getName().equals(name)) {
                return this.getTypeOllir(s.getType());
            }
        }

        // IMP unreachable
        return "";
    }

    private String getIdentifierOllir(String tabs, JmmNode node, boolean isAux) {
        String name = node.get("name");
        // local variable
        for (Symbol s : this.localVars) {
            if (s.getName().equals(name)) {
                return this.getSymbolOllir(s);
            }
        }
        // method parameter
        for (int i = 0; i < this.parameters.size(); ++i) {
            Symbol s = this.parameters.get(i);
            if (s.getName().equals(name)) {
                return "$" + i + "." + this.getSymbolOllir(s);
            }
        }
        // class field
        for (Symbol s : this.symbolTable.getFields()) {
            if (s.getName().equals(name)) {
                // ganhamos! A angola e nossa!
                // if is aux, getfield needs to be stored on temp var
                String typeOllir = this.getTypeOllir(s.getType());
                String ret = "getfield(this, " + this.getSymbolOllir(s) + ")" + typeOllir;
                if (isAux)
                    return this.injectTempVar(tabs, typeOllir, ret);
                return ret;
            }
        }

        // TODO import
        return "";
    }

    private String getLiteralOllir(String tabs, JmmNode node, boolean isAux) {
        switch (node.get("type")) {
            case "boolean":
                return (node.get("value").equals("true") ? "1" : "0") + ".bool";
            case "int":
                return node.get("value") + ".i32";
            case "this":
                return "this";
            case "identifier":
                return this.getIdentifierOllir(tabs, node, isAux);
            default:
                return "";
        }
    }

    private String getNewOllir(String tabs, JmmNode node, boolean isAux) {
        String ret = "new(";
        String type;

        if (node.get("type").equals("array")) {
            type = ".array.i32";
            ret += "array, " + this.getOpOllir(tabs, node.getChildren().get(0), true) + ")" + type;

            if (isAux) {
                ret = this.injectTempVar(tabs, type, ret);
            }
        } else {  // class object
            String name = node.get("name");
            type = "." + name;
            ret += name + ")" + type;

            if (isAux) {
                ret = this.injectTempVar(tabs, type, ret);
                this.ollirCode.append(tabs).append("invokespecial(").append(ret).append(", \"<init>\").V\n");
            }
        }


        return ret;
    }

    private boolean varIsClassField(String name) {
        return !this.localVars.stream().anyMatch(v -> v.getName().equals(name)) &&
                !this.parameters.stream().anyMatch(v -> v.getName().equals(name)) &&
                this.symbolTable.getFields().stream().anyMatch(v -> v.getName().equals(name));
    }

    private String getAssignOllir(String tabs, JmmNode node) {
        JmmNode leftChild = node.getChildren().get(0),
                rightChild = node.getChildren().get(1);
        String assigneeNome = leftChild.get("name");
        boolean isField = this.varIsClassField(assigneeNome);
        String type = this.getVarType(assigneeNome);

        // assignee
        String assignee = assigneeNome;
        // if is array access
        if (leftChild.get("isArrayAccess").equals("yes")) {
            assignee += "[" + this.getOpOllir(tabs, leftChild.getChildren().get(0), true) + "]";
            // remove the array part of the type (.array.i32 -> .i32)
            type = "." + type.split("\\.")[2];
        }
        assignee += type;

        // content (on fields, it needs to have an aux var)
        String content = this.getOpOllir(tabs, rightChild, isField).trim();

        String ret;
        if (isField) {
            ret = "putfield(this, " + assignee + ", " + content + ")";
        } else {
            ret = assignee + " :=" + type + " " + content;
        }

        // classes need to be instantiated
        if (rightChild.getKind().equals("New") && rightChild.get("type").equals("class"))
            ret += ";\n" + tabs + "invokespecial(" + assigneeNome + type + ", \"<init>\").V";

        return ret;
    }

    private String getOpOllir(String tabs, JmmNode node, boolean isAux) {
        String ret = "";
        if (!isAux)
            ret += tabs;

        switch (node.getKind()) {
            case "Binary":
                ret += this.getBinaryOllir(tabs, node, isAux);
                break;
            case "Unary":
                ret += this.getUnaryOllir(tabs, node, isAux);
                break;
            case "Literal":
                ret += this.getLiteralOllir(tabs, node, isAux);
                break;
            case "New":
                ret += this.getNewOllir(tabs, node, isAux);
                break;
            case "Assign":
                ret += this.getAssignOllir(tabs, node);
                break;
        }

        return ret;
    }

    private String getOpOllir(String tabs, JmmNode node) {
        return this.getOpOllir(tabs, node, false);
    }
}
