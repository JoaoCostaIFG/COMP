import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class OllirEmitter {
    private final MySymbolTable symbolTable;
    private final StringBuilder ollirCode;
    private int labelCount;
    private int auxCount;
    private List<Symbol> localVars, parameters;
    private final Stack<String> contextStack;

    public OllirEmitter(MySymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.ollirCode = new StringBuilder();
        this.auxCount = 0;
        this.labelCount = 0;
        this.localVars = new ArrayList<>();
        this.parameters = new ArrayList<>();
        this.contextStack = new Stack<>();
    }

    public String getOllirCode() {
        return this.ollirCode.toString();
    }

    // TODO do more
    private String encode(String in) {
        String out = in.replace("d", "dd");
        return out.replace("$", "d");
    }

    private String getNextAuxVar() {
        return "aux" + (this.auxCount++);
    }

    private String getNextLabel(String pre) {
        return pre + (this.labelCount++);
    }

    private void loadMethod(Method method) {
        this.localVars = method.getLocalVars();
        this.parameters = method.getParameters();
        // reset label and auxiliar variables counters
        this.auxCount = 0;
        this.labelCount = 0;
    }

    private String ollirNameTrim(String typeOllir) {
        return typeOllir.replaceFirst("(.*?)\\." +
                ((typeOllir.charAt(0) == '$') ? "(.*?)\\." : ""), "");
    }

    private Type getTypeFromOllir(String typeOllir) {
        String ollirNameTrim = this.ollirNameTrim(typeOllir);

        String[] split = ollirNameTrim.split("\\.");
        switch (split[split.length - 1]) {
            case "i32":
                return new Type("int", split.length >= 2);
            case "bool":
                return new Type("boolean", split.length >= 2);
            default:
                return new Type(split[split.length - 1], split.length >= 2);
        }
    }

    private String primitiveType(Type type) {
        switch (type.getName()) {
            case "int":
                return "i32";
            case "boolean":
                return "bool";
            case "void":
                return "V";
            default:
                return null;
        }
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

    public String visit(JmmNode node) {
        this.ollirCode.setLength(0);  // clear length to allow reuse

        String className = this.encode(this.symbolTable.getClassName());
        this.ollirCode.append(className).append(" {\n");
        // TODO extends

        // class fields
        for (Symbol field : this.symbolTable.getFields()) {
            this.ollirCode.append("\t.field private ").append(this.getSymbolOllir(field)).append(";\n");
        }

        // constructor
        this.ollirCode.append("\t.construct ").append(className).append("().V {\n");
        this.ollirCode.append("\t\tinvokespecial(this, \"<init>\").V;\n");
        this.ollirCode.append("\t}\n");

        for (String methodName : this.symbolTable.getMethods()) {
            this.getMethodOllir(methodName);
        }

        this.ollirCode.append("}");
        return this.getOllirCode();
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

        if (methodRetNode == null) { // no return statement (void method)
            // TODO ?
            // this.ollirCode.append(tabs).append("\t").append("ret.V;\n");
        } else {
            String retOllir = this.getOpOllir(tabs + "\t", methodRetNode.getChildren().get(0));
            this.ollirCode.append(tabs).append("\t")
                    .append("ret").append(this.getTypeOllir(method.getReturnType()))
                    .append(" ").append(retOllir.trim()).append(";\n");
        }

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

    public String getCondOllir(String tabs, JmmNode n) {
        boolean isBinOp = n.getKind().equals("Binary");
        String nodeOllir = this.getOpOllir(tabs, n, !isBinOp).trim();
        if (!isBinOp) {  // conditions have to be operations (binary)
            return nodeOllir + " && 1.bool";
        }
        return nodeOllir;
    }

    private void getWhileOllir(String tabs, JmmNode n) {
        JmmNode condNode = n.getChildren().get(0);
        JmmNode body = n.getChildren().get(1);

        String loopLabel = this.getNextLabel("Loop");
        String endLabel = this.getNextLabel("EndLoop");

        this.ollirCode.append(tabs).append(loopLabel).append(":\n");
        // condition
        this.contextStack.push(".bool");
        String condOllir = this.getCondOllir(tabs + "\t", condNode.getChildren().get(0));
        this.contextStack.pop();
        this.ollirCode.append(tabs).append("\t")
                .append("if (").append(condOllir).append(") goto ").append(endLabel).append(";\n");
        // body
        this.getBodyOllir(tabs + "\t", body);
        // make it loopar
        this.ollirCode.append(tabs).append("\t")
                .append("goto ").append(loopLabel).append(";\n");
        // end loop
        this.ollirCode.append(tabs).append(endLabel).append(":\n");
    }

    public void getIfOllir(String tabs, JmmNode n) {
        JmmNode condNode = n.getChildren().get(0);
        JmmNode body = n.getChildren().get(1);
        JmmNode elseBody = n.getChildren().get(2);

        this.contextStack.push(".bool");
        String condOllir = this.getCondOllir(tabs + "\t", condNode.getChildren().get(0));
        this.contextStack.pop();
        String bodyLabel = this.getNextLabel("Body");
        String endLabel = this.getNextLabel("Endif");

        // if condition
        this.ollirCode.append(tabs).append("if (").append(condOllir.trim())
                .append(") goto ").append(bodyLabel).append(";\n");
        // else
        this.getBodyOllir(tabs + "\t", elseBody);
        this.ollirCode.append(tabs).append("\tgoto ").append(endLabel).append(";\n");
        // if body
        this.ollirCode.append(tabs).append(bodyLabel).append(":\n");
        this.getBodyOllir(tabs + "\t", body);
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
        this.contextStack.push(type);

        List<JmmNode> children = node.getChildren();
        String childLeftOllir = this.getOpOllir(tabs, children.get(0), true);
        String childRightOllir = this.getOpOllir(tabs, children.get(1), true);
        String ret = childLeftOllir + " " + op + type + " " + childRightOllir;

        this.contextStack.pop();
        if (isAux)
            return this.injectTempVar(tabs, type, ret);
        return ret;
    }

    private String getAndOllir(String tabs, JmmNode node, boolean isAux) {
        final String type = ".bool";
        this.contextStack.push(type);

        List<JmmNode> children = node.getChildren();
        String childLeftOllir = this.getOpOllir(tabs, children.get(0), true);
        String childRightOllir = this.getOpOllir(tabs, children.get(1), true);
        String ret = childLeftOllir + " &&" + type + " " + childRightOllir;

        this.contextStack.pop();
        if (isAux)
            return this.injectTempVar(tabs, type, ret);
        return ret;
    }

    private String getLessThanOllir(String tabs, JmmNode node, boolean isAux) {
        final String retType = ".bool", type = ".i32";
        this.contextStack.push(type);

        List<JmmNode> children = node.getChildren();
        String childLeftOllir = this.getOpOllir(tabs, children.get(0), true);
        String childRightOllir = this.getOpOllir(tabs, children.get(1), true);
        String ret = childLeftOllir + " <" + type + " " + childRightOllir;

        this.contextStack.pop();
        if (isAux)
            return this.injectTempVar(tabs, retType, ret);
        return ret;
    }

    private boolean identIsImport(String name) {
        return this.localVars.stream().noneMatch(v -> v.getName().equals(name)) &&
                this.parameters.stream().noneMatch(v -> v.getName().equals(name)) &&
                this.symbolTable.getFields().stream().noneMatch(v -> v.getName().equals(name));
    }

    private String getDotOllir(String tabs, JmmNode node, boolean isAux) {
        List<JmmNode> children = node.getChildren();
        JmmNode leftChild = children.get(0);
        JmmNode rightChild = children.get(1);
        StringBuilder ret;
        String type = "";

        if (rightChild.getKind().equals("Len")) {
            type = ".i32";
            ret = new StringBuilder("arraylength(" + this.getOpOllir(tabs, leftChild, true) + ")" + type);
        } else { // func call
            // invoquestatic on imports
            if (leftChild.getKind().equals("Literal") &&
                    leftChild.get("type").equals("identifier") &&
                    this.identIsImport(leftChild.get("name"))) {
                ret = new StringBuilder("invokestatic(");
            } else {  // invoke virtual on class instances
                ret = new StringBuilder("invokevirtual(");
            }

            // method name and class instance
            String methodName = rightChild.get("methodName");
            String leftChildOllir = this.getOpOllir(tabs, leftChild, true);
            ret.append(leftChildOllir).append(", \"").append(methodName).append("\"");

            // func call args
            List<Type> paramTypes = new ArrayList<>();
            if (rightChild.getNumChildren() > 0) {  // if the call has arguments
                for (JmmNode argNode : rightChild.getChildren().get(0).getChildren()) {
                    String opOllir = this.getOpOllir(tabs, argNode, true);
                    ret.append(", ").append(opOllir);
                    paramTypes.add(this.getTypeFromOllir(opOllir));
                }
            }
            ret.append(")");

            Method callMethod = null;
            if (leftChildOllir.equals("this") ||
                    this.getTypeFromOllir(leftChildOllir).getName().equals(this.symbolTable.getClassName())) {
                // only search for methods when the class is the target
                callMethod = this.symbolTable.getMethodByCall(methodName, paramTypes);
            }
            if (callMethod == null) {  // infer return type
                if (this.contextStack.empty())
                    ret.append(".V");
                else
                    ret.append(this.contextStack.peek());
            } else {  // found the method
                ret.append(this.getTypeOllir(callMethod.getReturnType()));
            }
        }

        if (isAux)
            return this.injectTempVar(tabs, type, ret.toString());
        return ret.toString();
    }

    private String getIndexOllir(String tabs, JmmNode node, boolean isAux) {
        final String type = ".i32";
        // final String type = "." + this.getTypeFromOllir(this.getOpOllir(tabs, children.get(0)).trim()).getName();
        List<JmmNode> children = node.getChildren();

        this.contextStack.push(".array" + type);
        String childLeftOllir = this.getOpOllir(tabs, children.get(0), true);
        this.contextStack.pop();
        this.contextStack.push(".i32");
        String childRightOllir = this.getOpOllir(tabs, children.get(1), true);
        this.contextStack.pop();

        // IMP array access have always to be stored in a temporary variable before usage
        // we are splitting the left child by "." on the left because we don't want the type to show
        String[] loSplit = childLeftOllir.split("\\.");
        String ret = loSplit[0];
        if (childLeftOllir.charAt(0) == '$')
            ret += "." + loSplit[1];
        ret += "[" + childRightOllir + "]" + type;
        if (isAux)
            return this.injectTempVar(tabs, type, ret);
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
            case "AND":
                return this.getAndOllir(tabs, node, isAux);
            case "LESSTHAN":
                return this.getLessThanOllir(tabs, node, isAux);
            case "DOT":
                return this.getDotOllir(tabs, node, isAux);
            case "INDEX":
                return this.getIndexOllir(tabs, node, isAux);
            default:
                return "";
        }
    }

    private String getUnaryOllir(String tabs, JmmNode node, boolean isAux) {
        // TODO invert expressions, e.g.: < --> >=
        final String type = ".bool";
        this.contextStack.push(".bool");

        JmmNode child = node.getChildren().get(0);
        String childOllir = this.getOpOllir(tabs, child, true);
        String ret = childOllir + " !" + type + " " + childOllir;

        this.contextStack.pop();
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
                return this.encode(this.getSymbolOllir(s));
            }
        }
        // method parameter
        for (int i = 0; i < this.parameters.size(); ++i) {
            Symbol s = this.parameters.get(i);
            if (s.getName().equals(name)) {
                return "$" + i + "." + this.encode(this.getSymbolOllir(s));
            }
        }
        // class field
        for (Symbol s : this.symbolTable.getFields()) {
            if (s.getName().equals(name)) {
                // ganhamos! A angola e nossa!
                // if is aux, getfield needs to be stored on temp var
                String typeOllir = this.getTypeOllir(s.getType());
                String ret = "getfield(this, " + this.encode(this.getSymbolOllir(s)) + ")" + typeOllir;
                if (isAux)
                    return this.injectTempVar(tabs, typeOllir, ret);
                return ret;
            }
        }

        // has to be an import
        return node.get("name");
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
            this.contextStack.push(".array.i32");
            ret += "array, " + this.getOpOllir(tabs, node.getChildren().get(0), true) + ")" + type;
            this.contextStack.pop();

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
        return this.localVars.stream().noneMatch(v -> v.getName().equals(name)) &&
                this.parameters.stream().noneMatch(v -> v.getName().equals(name)) &&
                this.symbolTable.getFields().stream().anyMatch(v -> v.getName().equals(name));
    }

    private String getAssignOllir(String tabs, JmmNode node) {
        JmmNode leftChild = node.getChildren().get(0),
                rightChild = node.getChildren().get(1);
        String assigneeName = leftChild.get("name");
        boolean isField = this.varIsClassField(assigneeName);
        String type = this.getVarType(assigneeName);

        // assignee
        String assignee = this.encode(assigneeName);
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
            ret += ";\n" + tabs + "invokespecial(" + this.encode(assigneeName) + type + ", \"<init>\").V";

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
