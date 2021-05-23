import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.*;

public class OllirEmitter {
    private static final String regPrefix = "t";
    // for constant folding and constant propagation
    private static final boolean doOptimizations = false;

    private final MySymbolTable symbolTable;
    private final StringBuilder ollirCode;
    private int labelCount;
    private int auxCount;
    private List<Symbol> localVars, parameters;
    private final Map<String, String> sanitizationMap;
    private final Stack<String> contextStack; // used to infer functions types (that aren't part of our class)
    private Map<String, Integer> constantTable; // used for constant propagation

    public OllirEmitter(MySymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.ollirCode = new StringBuilder();
        this.auxCount = 0;
        this.labelCount = 0;
        this.localVars = new ArrayList<>();
        this.parameters = new ArrayList<>();
        this.sanitizationMap = new HashMap<>();
        this.contextStack = new Stack<>();
        this.constantTable = new HashMap<>();
    }

    public String getOllirCode() {
        return this.ollirCode.toString();
    }

    private String getNextAuxVar() {
        return OllirEmitter.regPrefix + (this.auxCount++);
    }

    private String sanitizeSymbol(String varName) {
        if (this.sanitizationMap.containsKey(varName))
            return this.sanitizationMap.get(varName);
        this.sanitizationMap.put(varName, this.getNextAuxVar());
        return this.sanitizationMap.get(varName);
    }

    private String getNextLabel(String pre) {
        return pre + (this.labelCount++);
    }

    private String[] getLabelPair(String... pres) {
        String[] ret = new String[pres.length];
        for (int i = 0; i < pres.length; ++i)
            ret[i] = pres[i] + this.labelCount;
        ++this.labelCount;
        return ret;
    }

    private void loadMethod(Method method) {
        this.localVars = method.getLocalVars();
        this.parameters = method.getParameters();
        // reset label and auxiliar variables counters
        this.auxCount = 0;
        this.labelCount = 0;
        // reset sanitization fields
        this.sanitizationMap.clear();
        for (Symbol s : this.symbolTable.getFields())
            this.sanitizationMap.put(s.getName(), this.getNextAuxVar());
        for (Symbol s : this.parameters)
            this.sanitizationMap.put(s.getName(), this.getNextAuxVar());
        // reset constant table
        this.constantTable.clear();
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
        return this.sanitizeSymbol(var.getName()) + this.getTypeOllir(var.getType());
    }

    public String visit(JmmNode node) {
        this.ollirCode.setLength(0);  // clear length to allow reuse

        // imports
        for (String i : this.symbolTable.getImports())
            this.ollirCode.append("import ").append(i).append(";\n");
        this.ollirCode.append("\n");

        String className = this.symbolTable.getClassName();
        this.ollirCode.append(className);
        if (this.symbolTable.getSuper() != null)
            this.ollirCode.append(" extends ").append(this.symbolTable.getSuper());
        this.ollirCode.append(" {\n");

        // class fields
        for (Symbol field : this.symbolTable.getFields()) {
            this.ollirCode.append("\t.field private ").append(this.getSymbolOllir(field)).append(";\n");
        }

        // constructor
        this.ollirCode.append("\t.construct ").append(className).append("().V {\n");
        this.ollirCode.append("\t\tinvokespecial(this, \"<init>\").V;\n");
        this.ollirCode.append("\t}\n");

        for (String methodName : this.symbolTable.getMethods()) {
            this.ollirCode.append("\n");
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
            this.ollirCode.append(tabs).append("\t").append("ret.V;\n");
        } else {
            String retOllir = this.getOpOllir(tabs + "\t", methodRetNode.getChildren().get(0), true);
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
                    if (doOptimizations)
                        this.getDoWhileOllir(tabs, n);
                    else
                        this.getWhileOllir(tabs, n);
                    break;
            }
        }
    }

    public String getCondOllir(String tabs, JmmNode n, boolean isNegated) {
        String opKind = n.getKind();
        // alternative NOT style
        // boolean isBinOp = (opKind.equals("Binary") && !n.get("op").equals("DOT")) ||
        //         opKind.equals("Unary");
        boolean isBinOp = opKind.equals("Binary") && !n.get("op").equals("DOT");

        String nodeOllir;
        if (isNegated) {
            // in this case, the condition needs to be negated in order to evaluate it as an IfFalse
            nodeOllir = this.getOpOllir(tabs, n, true);
            return this.injectTempVar(tabs, ".bool", "!.bool " + nodeOllir) + " &&.bool 1.bool";
        } else {
            nodeOllir = this.getOpOllir(tabs, n, !isBinOp);
            if (!isBinOp)  // conditions have to be operations (binary)
                nodeOllir += " &&.bool 1.bool";
        }

        return nodeOllir.trim();
    }

    public String getCondOllir(String tabs, JmmNode n) {
        return this.getCondOllir(tabs, n, false);
    }

    /* Old version of the while loop (before DoWhile optimization) */
    private void getWhileOllir(String tabs, JmmNode n) {
        JmmNode condNode = n.getChildren().get(0);
        JmmNode body = n.getChildren().get(1);

        String[] labels = this.getLabelPair("Loop", "EndLoop");
        String loopLabel = labels[0];
        String endLabel = labels[1];

        this.ollirCode.append(tabs).append(loopLabel).append(":\n");
        // condition (IMP this if has to be interpreted as an ifFalse)
        this.contextStack.push(".bool");
        String condOllir = this.getCondOllir(tabs + "\t", condNode.getChildren().get(0));
        this.contextStack.pop();
        this.ollirCode.append(tabs).append("\t")
                .append("if (").append(condOllir).append(") goto ").append(endLabel).append(";\n");
        // body
        this.getBodyOllir(tabs + "\t", body);
        this.ollirCode.append(tabs).append("\t") // make it loopar
                .append("goto ").append(loopLabel).append(";\n");
        // end loop
        this.ollirCode.append(tabs).append(endLabel).append(":\n");
    }

    /* New version of the while loop (after DoWhile optimization) */
    private void getDoWhileOllir(String tabs, JmmNode n) {
        JmmNode condNode = n.getChildren().get(0);
        JmmNode body = n.getChildren().get(1);

        String[] labels = this.getLabelPair("Loop", "EndLoop");
        String loopLabel = labels[0];
        String endLabel = labels[1];

        // initial cond (IMP this if has to be interpreted as an ifFalse)
        this.contextStack.push(".bool");
        String condOllir = this.getCondOllir(tabs, condNode.getChildren().get(0));
        this.contextStack.pop();
        this.ollirCode.append(tabs).append("if (").append(condOllir).append(") goto ").append(endLabel).append(";\n");

        // body
        this.rmAssignedConstants(body);

        this.ollirCode.append(tabs).append(loopLabel).append(":\n");
        this.getBodyOllir(tabs + "\t", body);
        // inner condition (IMP this if has to be interpreted as an ifFalse so we negate it)
        this.contextStack.push(".bool");
        String innerCondOllir = this.getCondOllir(tabs + "\t", condNode.getChildren().get(0), true);
        this.contextStack.pop();
        this.ollirCode.append(tabs).append("\t") // make it loopar
                .append("if (").append(innerCondOllir).append(") goto ").append(loopLabel).append(";\n");

        // end loop
        this.ollirCode.append(tabs).append(endLabel).append(":\n");
    }

    public void getIfOllir(String tabs, JmmNode n) {
        JmmNode condNode = n.getChildren().get(0);
        JmmNode body = n.getChildren().get(1);
        JmmNode elseBody = n.getChildren().get(2);

        this.contextStack.push(".bool");
        String condOllir = this.getCondOllir(tabs, condNode.getChildren().get(0));
        this.contextStack.pop();
        String[] labels = this.getLabelPair("Else", "Endif");
        String elseLabel = labels[0];
        String endLabel = labels[1];

        // condition (IMP this if has to be interpreted as an ifFalse)
        this.ollirCode.append(tabs).append("if (").append(condOllir.trim())
                .append(") goto ").append(elseLabel).append(";\n");

        // backup constant table between body parsings to prevent conflicts
        var constTableBackup = this.constantTable;
        // if body
        this.constantTable = new HashMap<>(constTableBackup);  // reset constant table
        this.getBodyOllir(tabs + "\t", body);
        this.ollirCode.append(tabs).append("\tgoto ").append(endLabel).append(";\n");
        // else
        this.constantTable = new HashMap<>(constTableBackup);  // reset constant table
        this.ollirCode.append(tabs).append(elseLabel).append(":\n");
        this.getBodyOllir(tabs + "\t", elseBody);
        this.ollirCode.append(tabs).append(endLabel).append(":").append("\n");

        this.constantTable = constTableBackup;
        this.rmAssignedConstants(body, elseBody);
    }

    private void rmAssignedConstants(JmmNode... nodes) {
        if (!doOptimizations)
            return;

        for (JmmNode n : nodes) {
            for (JmmNode child : n.getChildren()) {
                if (child.getKind().equals("Assign")) {
                    String name = child.getChildren().get(0).get("name");
                    this.constantTable.remove(name);
                } else if (child.getNumChildren() > 0) {
                    this.rmAssignedConstants(child);
                }
            }
        }
    }

    private String injectTempVar(String tabs, String type, String content) {
        String auxVarName = this.getNextAuxVar() + type;
        this.ollirCode.append(tabs).append(auxVarName).append(" :=")
                .append(type).append(" ").append(content).append(";\n");
        return auxVarName;
    }

    private boolean stringIsInt(String str) {
        return str.split("\\.")[0].matches("-?\\d+");
    }

    private String getIntOpOllir(String tabs, JmmNode node, String op, boolean isAux) {
        final String type = ".i32";
        this.contextStack.push(type);

        List<JmmNode> children = node.getChildren();
        String childLeftOllir = this.getOpOllir(tabs, children.get(0), true);
        String childRightOllir = this.getOpOllir(tabs, children.get(1), true);
        this.contextStack.pop();

        // both children are constants => Constant Folding
        if (doOptimizations && this.stringIsInt(childLeftOllir) && this.stringIsInt(childRightOllir)) {
            switch (op) {
                case "+":
                    return (Integer.parseInt(childLeftOllir.split("\\.")[0]) +
                            Integer.parseInt(childRightOllir.split("\\.")[0])) + type;
                case "-":
                    return (Integer.parseInt(childLeftOllir.split("\\.")[0]) -
                            Integer.parseInt(childRightOllir.split("\\.")[0])) + type;
                case "*":
                    return (Integer.parseInt(childLeftOllir.split("\\.")[0]) *
                            Integer.parseInt(childRightOllir.split("\\.")[0])) + type;
                case "/":
                    return (Integer.parseInt(childLeftOllir.split("\\.")[0]) /
                            Integer.parseInt(childRightOllir.split("\\.")[0])) + type;
                default:
                    break;
            }
        }

        String ret = childLeftOllir + " " + op + type + " " + childRightOllir;
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
        String type;

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
                    type = ".V";
                else
                    type = this.contextStack.peek();
            } else {  // found the method
                type = this.getTypeOllir(callMethod.getReturnType());
            }
            ret.append(type);
        }

        if (isAux)
            return this.injectTempVar(tabs, type, ret.toString());
        return ret.toString();
    }

    private String getIndexOllir(String tabs, JmmNode node, boolean isAux) {
        final String indexType = ".i32";
        // TODO I dont't like this
        String tmpType = this.primitiveType(this.getTypeFromOllir(this.getOpOllir("", node.getChildren().get(0))));
        final String type = "." + (tmpType == null ? "String" : tmpType);
        List<JmmNode> children = node.getChildren();

        this.contextStack.push(".array" + type);
        String childLeftOllir = this.getOpOllir(tabs, children.get(0), true);
        this.contextStack.pop();
        this.contextStack.push(indexType);
        String childRightOllir = this.getOpOllir(tabs, children.get(1), true);
        this.contextStack.pop();

        // IMP this is a hack to prevent the use of constants as array indexes. They need to be stored in variables
        if (children.get(1).getKind().equals("Literal") && children.get(1).get("type").equals("int"))
            childRightOllir = this.injectTempVar(tabs, indexType, childRightOllir);

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
        // alternative NOT style
        //String ret = childOllir + " !" + type + " " + childOllir;
        String ret = "!" + type + " " + childOllir;

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
                // search in constant table (Constant propagation)
                if (doOptimizations && this.constantTable.containsKey(name))
                    return this.constantTable.get(name) + this.getTypeOllir(s.getType());
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

        // has to be an import
        return node.get("name");
    }

    private String getLiteralOllir(String tabs, JmmNode node, boolean isAux) {
        switch (node.get("type")) {
            case "boolean":
                return (node.get("value").equals("true") ? "1" : "0") + ".bool";
            case "int":
                String value = node.get("value")
                        .replace("_", "")
                        .replace("l", "")
                        .replace("L", "");
                return Integer.decode(value) + ".i32";
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
                this.ollirCode.append(tabs).append("invokespecial(").append(ret).append(", \"<init>\").V;\n");
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
        // remove from constant table since this is a new assignment
        this.constantTable.remove(assigneeName);

        boolean isField = this.varIsClassField(assigneeName);
        String type = this.getVarType(assigneeName);

        // assignee
        assigneeName = this.sanitizeSymbol(assigneeName);
        String assignee = assigneeName;

        // if is array access
        boolean isArrayAccess = leftChild.get("isArrayAccess").equals("yes");
        if (isArrayAccess) {
            // load array reference is needed
            if (isField) {
                String content = "getfield(this, " + assignee + type + ")" + type;
                assignee = this.injectTempVar(tabs, type, content).split("\\.")[0];
            }
            // remove the array part of the type (.array.i32 -> .i32)
            type = "." + type.split("\\.")[2];

            JmmNode indexChild = leftChild.getChildren().get(0);
            String indexOllir = this.getOpOllir(tabs, indexChild, true);
            // IMP this is a hack to prevent the use of constants as array indexes. They need to be stored in variables
            if (indexChild.getKind().equals("Literal") && indexChild.get("type").equals("int"))
                indexOllir = this.injectTempVar(tabs, type, indexOllir);

            assignee += "[" + indexOllir + "]";
        }
        assignee += type;

        // content (on fields, it needs to have an aux var)
        this.contextStack.push(type);
        String content = this.getOpOllir(tabs, rightChild, isField).trim();
        // Constant Propagation
        if (doOptimizations && this.stringIsInt(content) && !isArrayAccess) {
            this.constantTable.put(leftChild.get("name"), Integer.valueOf(content.split("\\.")[0]));
        }
        this.contextStack.pop();

        String ret;
        if (isField && !isArrayAccess) {
            ret = "putfield(this, " + assignee + ", " + content + ").V";
        } else {
            ret = assignee + " :=" + type + " " + content;
        }

        // obrigado gabide, es o maior <3
        // classes need to be instantiated
        if (rightChild.getKind().equals("New") && rightChild.get("type").equals("class"))
            ret += ";\n" + tabs + "invokespecial(" + assigneeName + type + ", \"<init>\").V";

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
