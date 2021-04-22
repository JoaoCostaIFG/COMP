import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.ollir.OllirUtils;

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
    public String elemTypeJasmin(ElementType type) {
        switch (type) {
            case INT32:
                return "I";
            case BOOLEAN:
                return "Z";
            case STRING:
                return "Ljava/lang/String;";
            case VOID:
                return "V";
            default:
                return type.toString();
        }
    }

    public String elemTypeJasmin(Type type) {
        switch (type.getTypeOfElement()) {
            case ARRAYREF:
                ArrayType aType = (ArrayType) type;
                StringBuilder ret = new StringBuilder();
                if (aType.getNumDimensions() > 0) ret.append("[");
                ret.append(this.elemTypeJasmin(aType.getTypeOfElements()));
                return ret.toString();
            case OBJECTREF:
                return "";
            case CLASS:
                ClassType cType = (ClassType) type;
                return "L" + cType.getName() + ";";
            case THIS:
                return "";
            default:
                return this.elemTypeJasmin(type.getTypeOfElement());
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

        for (Method method : this.ollirClass.getMethods()) {
            this.methodJasmin(method);
        }

        return this.getJasminCode();
    }

    public void standardInitializerJasmin() {
        this.jasminCode.append("; standard initializer\n")
                .append(".method public <init>()V\n")
                .append("\taload_0\n")
                .append("\tinvokespecial java/lang/Object.<init>()V\n")
                .append("\treturn\n")
                .append(".end method\n");
    }

    public void methodJasmin(Method method) {
        if (method.isConstructMethod()) {
            // TODO this is a hack
            this.standardInitializerJasmin();
            return;
        }

        this.jasminCode.append(".method public ");
        if (method.isStaticMethod()) this.jasminCode.append("static ");
        this.jasminCode.append(method.getMethodName());

        // method signature (args)
        this.jasminCode.append("(");
        for (Element e : method.getParams()) {
            this.jasminCode.append(this.elemTypeJasmin(e.getType()));
        }
        this.jasminCode.append(")");

        Type retType = method.getReturnType();
        this.jasminCode.append(this.elemTypeJasmin(retType)).append("\n");

        String tabs = "\t";
        for (Instruction i : method.getInstructions()) {
            this.instructionJasmin(tabs, i);
        }

        // return
        this.jasminCode.append(tabs).append(this.instrPreJasmin(retType)).append("return\n");
        this.jasminCode.append(".end method\n");
    }

    public void instructionJasmin(String tabs, Instruction instr) {
        switch (instr.getInstType()) {
            case ASSIGN:
                break;
            case CALL:
                this.callInstructionJasmin(tabs, (CallInstruction) instr);
                break;
            case GOTO:
                break;
            case BRANCH:
                break;
            case RETURN:
                break;
            case PUTFIELD:
                break;
            case GETFIELD:
                break;
            case UNARYOPER:
                break;
            case BINARYOPER:
                break;
            case NOPER:
                break;
        }
    }

    private String callArg(Element e) {
        if (e.isLiteral()) { // if the e is not a literal, then it is a variable
            return ((LiteralElement) e).getLiteral().replace("\"", "");
        } else {
            Operand o = (Operand) e;
            // o1.getType()
            return o.getName();
        }
    }

    private void callInstructionJasmin(String tabs, CallInstruction instr) {
        this.jasminCode.append(tabs);

        CallType invType = OllirUtils.getCallInvocationType(instr);
        switch (invType) {
            case invokevirtual:
                break;
            case invokeinterface:
                break;
            case invokespecial:
                break;
            case invokestatic:
                this.jasminCode.append("invokestatic ");
                break;
            case NEW:
                break;
            case arraylength:
                break;
            case ldc:
                break;
        }

        this.jasminCode.append(this.callArg(instr.getFirstArg()));

        if (instr.getNumOperands() > 1) {
            if (invType != CallType.NEW) { // only new type instructions do not have a field with second arg
                this.jasminCode.append(".").append(this.callArg(instr.getSecondArg()));
            }

            // args
            this.jasminCode.append("(");
            for (Element arg : instr.getListOfOperands()) {
                this.jasminCode.append(this.callArg(arg));
            }
            this.jasminCode.append(")");
        }

        this.jasminCode.append(this.elemTypeJasmin(instr.getReturnType())).append("\n");
    }
}
