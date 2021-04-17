import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;

import java.util.List;

public class OllirEmitter extends PreorderJmmVisitor<Boolean, String> {
    private final MySymbolTable symbolTable;
    private final StringBuilder ollirCode;
    private int labelCount;
    private Integer auxCount;

    public OllirEmitter(MySymbolTable symbolTable) {
        super();
        this.symbolTable = symbolTable;
        this.ollirCode = new StringBuilder();
        this.auxCount = 0;
        this.labelCount = 0;
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

    private String primitiveType(Type type) {
        if (type.getName().equals("int"))
            return "i32";
        else if (type.getName().equals("boolean"))
            return "bool";
        else
            return null;
    }

    private String getTypeOllir(Type type) {
        String ret = "";
        if (type.isArray()) {
            ret = "array.";
        }

        String primitiveType = this.primitiveType(type);
        if (primitiveType == null)
            ret += type.getName();
        else
            ret += primitiveType;

        return ret;
    }

    private String getSymbolOllir(Symbol var) {
        return var.getName() + "." + this.getTypeOllir(var.getType());
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

        // TODO iterar pelos filhos instead?
        for (String methodName : this.symbolTable.getMethods()) {
            this.getMethodOllir(methodName);
        }

        this.ollirCode.append("}");
        return this.ollirCode.toString();
    }

    private void getMethodOllir(String methodId) {
        String methodName = Method.getNameFromString(methodId);
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
        this.ollirCode.append(").").append(this.getTypeOllir(this.symbolTable.getReturnType(methodId))).append(" {\n");

        // method body
        JmmNode methodNode = this.symbolTable.getMethod(methodId).getNode();
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
                case "Literal":
                case "Binary":
                case "Unary":
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
        JmmNode elseBody = n.getChildren().get(2);
        String condOllir = this.getOpOllir("", condNode.getChildren().get(0));
        String elseLabel = this.getNextLabel("else");
        String endLabel = this.getNextLabel("endif");
        // If condition
        this.ollirCode.append(tabs).append("if (").append(condOllir).append(") goto ").append(elseLabel).append(";\n");
        // If BOdy
        this.getBodyOllir(tabs + "\t", body);
        this.ollirCode.append(tabs).append("\tgoto ").append(endLabel).append(";\n");
        // Else
        this.ollirCode.append(tabs).append(elseLabel).append(":\n");
        this.getBodyOllir(tabs + "\t", elseBody);
        this.ollirCode.append(tabs).append(endLabel).append(":").append("\n");
    }

    public void getIfOllir(String tabs, JmmNode n) {
        JmmNode condNode = n.getChildren().get(0);
        JmmNode body = n.getChildren().get(1);
        JmmNode elseBody = n.getChildren().get(2);
        String condOllir = this.getOpOllir("", condNode.getChildren().get(0));
        String elseLabel = this.getNextLabel("else");
        String endLabel = this.getNextLabel("endif");
        // If condition
        this.ollirCode.append(tabs).append("if (").append(condOllir).append(") goto ").append(elseLabel).append(";\n");
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
                .append(type).append(" ").append(content).append("\n");
        return auxVarName;
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
            case "DOT":
                return this.getDotOllir(tabs, node, isAux);
            case "AND":
                return this.getAndOllir(tabs, node, isAux);
            case "LESSTHAN":
                return this.getLessThanOllir(tabs, node, isAux);
            default:
                return "";
        }
    }

    private String getUnaryOllir(String tabs, JmmNode node, boolean isAux) {
        final String type = ".bool";
        JmmNode child= node.getChildren().get(0);
        String childOllir = this.getOpOllir(tabs, child, true);
        String ret = childOllir + " !" + type + " " + childOllir;

        if (isAux)
            return this.injectTempVar(tabs, type, ret);
        return ret;
    }

    private String getIdentifierOllir(JmmNode node) {
        return node.get("name");
    }

    private String getLiteralOllir(JmmNode node) {
        switch (node.get("type")) {
            case "boolean":
                return (node.get("value").equals("true") ? "1" : "0") + ".bool";
            case "int":
                return node.get("value") + ".i32";
            case "this":
                return "this";
            case "identifier":
                return this.getIdentifierOllir(node);
            default:
                return "";
        }
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
                ret += this.getLiteralOllir(node);
                break;
        }

        return ret;
    }

    private String getOpOllir(String tabs, JmmNode node) {
        return this.getOpOllir(tabs, node, false);
    }
}
