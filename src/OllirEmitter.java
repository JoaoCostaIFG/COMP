import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;

import java.util.List;

public class OllirEmitter extends PreorderJmmVisitor<Boolean, String> {
    private final SymbolTable symbolTable;
    private String ollirCode;

    public OllirEmitter(SymbolTable symbolTable) {
        super();
        this.symbolTable = symbolTable;
        ollirCode = "";
        this.addVisit("Program", this::visitRoot);
    }

    public String getOllirCode() {
        return this.ollirCode;
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

    private String getVarOllir(Symbol var) {
        return var.getName() + "." + this.getTypeOllir(var.getType());
    }

    private String visitRoot(JmmNode node, Boolean ignored) {
        this.ollirCode += "class " + this.symbolTable.getClassName() + " {\n";
        // TODO extends

        // class fields
        for (Symbol field : this.symbolTable.getFields()) {
            this.ollirCode += "\t.field private " + this.getVarOllir(field) + ";\n";
        }

        // constructor
        this.ollirCode += "\t.construct " + this.symbolTable.getClassName() + "().V {\n";
        this.ollirCode += "\t\tinvokespecial(this, \"<init>\").V;\n";
        this.ollirCode += "\t}\n";

        for (String methodName : this.symbolTable.getMethods()) {
            this.ollirCode += this.getMethodOllir(methodName);
        }

        this.ollirCode += "}";
        return this.ollirCode;
    }

    private String getMethodOllir(String methodId) {
        String methodName = Method.getNameFromString(methodId);
        String tabs = "\t";
        String ret = tabs + ".method public ";
        if (methodName.equals("main"))
            ret += "static ";

        ret += methodName + "(";
        boolean first = true;
        for (Symbol param : this.symbolTable.getParameters(methodId)) {
            ret += (first ? "" : ", ") + this.getVarOllir(param);
            first = false;
        }
        ret += ")." + this.getTypeOllir(this.symbolTable.getReturnType(methodId)) + " {\n";

        ret += tabs + "}\n";
        return ret;
    }
}
