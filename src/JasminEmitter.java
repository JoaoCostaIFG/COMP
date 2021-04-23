import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.ollir.OllirUtils;

public class JasminEmitter {
    private final MySymbolTable symbolTable;
    private final ClassUnit ollirClass;
    private final StringBuilder jasminCode;
    private Method method;

    // TODO arithmetic

    // TODO super
    // TODO class fields
    // TODO assign
    // TODO arrays
    // TODO if
    // TODO while
    // TODO comps

    public JasminEmitter(MySymbolTable symbolTable, ClassUnit ollirClass) {
        this.symbolTable = symbolTable;
        this.ollirClass = ollirClass;
        this.jasminCode = new StringBuilder();
        this.method = null;
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
            case BOOLEAN:
                return "i";
            case VOID:
                return "";
            default: // TODO ?
                return "a";
        }
    }

    public String parse() {
        this.jasminCode.append("; Class accepted by Jasmin2.3\n");
        this.jasminCode.append(".class public ").append(this.ollirClass.getClassName()).append("\n");
        this.jasminCode.append(".super java/lang/Object\n");

        for (Method method : this.ollirClass.getMethods()) {
            this.method = method;
            this.methodJasmin(method);
        }

        return this.getJasminCode();
    }

    public void standardInitializerJasmin() {
        this.jasminCode.append("\n; standard initializer\n")
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

        this.jasminCode.append("\n.method public ");
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
                ((GotoInstruction) instr).show();
                break;
            case BRANCH:
                break;
            case RETURN:
                this.retInstructionJasmin(tabs, (ReturnInstruction) instr);
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
            default:
                break;
        }
    }

    private void loadCallArg(String tabs, Element arg) {
        String callArg = this.callArg(arg);
        String argStr = "";
        switch (arg.getType().getTypeOfElement()) {
            case INT32:
                if (Integer.parseInt(callArg) <= 5) {
                    argStr = "iconst_" + callArg;
                } else if (Integer.parseInt(callArg) <= 127) {
                    argStr = "bipush " + callArg;
                } else {
                    argStr = "ldc " + callArg;
                }
                break;
            case BOOLEAN:
                if (callArg.equals("1"))
                    argStr = "iconst_1";
                else
                    argStr = "iconst_0";
                break;
            case ARRAYREF:
                break;
            case OBJECTREF:
                System.out.println(callArg);
                break;
            case THIS:
                argStr = "aload_0";
                break;
            case STRING:
                argStr = "ldc " + callArg;
                break;
            case CLASS:
            default:
                // pls
                return;
        }

        this.jasminCode.append(tabs).append(argStr).append("\n");
    }

    private String callArg(Element e) {
        if (e.isLiteral()) { // if the e is not a literal, then it is a variable
            return ((LiteralElement) e).getLiteral().replace("\"", "");
        } else {
            Operand o = (Operand) e;
            // o1.getType()
            if (o.getName().equals("this"))
                return this.ollirClass.getClassName();
            return o.getName();
        }
    }

    private void callInstructionJasmin(String tabs, CallInstruction instr) {
        StringBuilder ret = new StringBuilder().append(tabs);

        CallType invType = OllirUtils.getCallInvocationType(instr);
        switch (invType) {
            case invokevirtual:
                ret.append("invokevirtual ");
                break;
            case invokeinterface:
                ret.append("invokeinterface ");
                break;
            case invokespecial:
                ret.append("invokespecial ");
                break;
            case invokestatic:
                ret.append("invokestatic ");
                break;
            case NEW:
                break;
            case arraylength:
                break;
            case ldc:
                break;
        }

        this.loadCallArg(tabs, instr.getFirstArg());
        ret.append(this.callArg(instr.getFirstArg()));

        if (instr.getNumOperands() > 1) {
            if (invType != CallType.NEW) { // only new type instructions do not have a field with second arg
                ret.append(".").append(this.callArg(instr.getSecondArg()));
            }

            // args
            ret.append("(");
            for (Element arg : instr.getListOfOperands()) {
                this.loadCallArg(tabs, arg);
                ret.append(this.elemTypeJasmin(arg.getType()));
            }
            ret.append(")");
        }

        this.jasminCode.append(ret).append(this.elemTypeJasmin(instr.getReturnType())).append("\n");
    }

    private void retInstructionJasmin(String tabs, ReturnInstruction instr) {
        this.jasminCode.append(tabs);

        Element opnd = instr.getOperand();
        if (opnd != null) {
            this.loadCallArg(tabs, opnd);
        }

        this.jasminCode.append(this.instrPreJasmin(this.method.getReturnType()))
                .append("return\n");
    }
}
