import org.specs.comp.ollir.*;
import org.specs.comp.ollir.Method;

public class JasminEmitter {
    private final MySymbolTable symbolTable;
    private final ClassUnit ollirClass;
    private final StringBuilder jasminCode;

    // TODO class fields
    // TODO super
    // TODO if
    // TODO while
    // TODO arithmetic
    // TODO comps
    // TODO assign
    // TODO arrays

    public JasminEmitter(MySymbolTable symbolTable, ClassUnit ollirClass) {
        this.symbolTable = symbolTable;
        this.ollirClass = ollirClass;
        this.jasminCode = new StringBuilder();
    }

    public String getJasminCode() {
        return this.jasminCode.toString();
    }

    // TODO complete this
    public String elemTypeJasmin(Type type) {
        switch (type.getTypeOfElement()) {
            case INT32:
                return "I";
            case VOID:
                return "V";
            default:
                return type.toString();
        }
    }

    public String instrPreJasmin(Type type) {
        switch (type.getTypeOfElement()) {
            case INT32:
                return "i";
            case VOID:
                return "";
            default:
                return type.toString();
        }
    }

    public String parse() {
        this.jasminCode.append("; Class accepted by Jasmin2.3\n");
        this.jasminCode.append(".class public ").append(this.ollirClass.getClassName()).append("\n");
        this.jasminCode.append(".super java/lang/Object\n\n");
        // always produce standard initializer
        this.standardInitializerJasmin();

        for (Method method : this.ollirClass.getMethods()) {
            this.methodJasmin(method);
        }

        return this.getJasminCode();
    }

    public void standardInitializerJasmin() {
        this.jasminCode.append("; standard initializer\n")
                .append(".method public <init>()V\n")
                .append("\taload_0\n")
                .append("\tinvokenonvirtual java/lang/Object/<init>()V\n")
                .append("\treturn\n")
                .append(".end method\n\n");
    }

    public void methodJasmin(Method method) {
        this.jasminCode.append(".method public ");
        if (method.isStaticMethod()) this.jasminCode.append("static ");
        this.jasminCode.append(method.getMethodName()).append("(");

        for (Element e : method.getParams()) {
            this.jasminCode.append(this.elemTypeJasmin(e.getType()));
        }

        Type retType = method.getReturnType();
        this.jasminCode.append(")").append(this.elemTypeJasmin(retType)).append("\n");

        String tabs = "\t";

        // return
        this.jasminCode.append(tabs).append(this.instrPreJasmin(retType)).append("return\n");
        this.jasminCode.append(".end method\n\n");
    }
}
