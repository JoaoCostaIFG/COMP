import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.ollir.OllirUtils;

import java.util.HashMap;
import java.util.Stack;

public class JasminEmitter {
    private final ClassUnit ollirClass;
    private final StringBuilder jasminCode;
    private HashMap<String, Descriptor> methodVarTable;
    private final Stack<Instruction> contextStack;

    // TODO arithmetic

    // TODO super
    // TODO class fields
    // TODO arrays
    // TODO if
    // TODO while
    // TODO comps

    public JasminEmitter(ClassUnit ollirClass) {
        this.ollirClass = ollirClass;
        this.jasminCode = new StringBuilder();
        this.methodVarTable = new HashMap<>();
        this.contextStack = new Stack<>();
    }

    public String getJasminCode() {
        return this.jasminCode.toString();
    }

    public void injectComment(String tabs, String... comments) {
        this.jasminCode.append(tabs).append(";");
        for (String c : comments)
            this.jasminCode.append(" ").append(c);
        this.jasminCode.append("\n");
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
                return this.ollirClass.getClassName();
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
            this.methodVarTable = OllirAccesser.getVarTable(method);
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
        // TODO
        // stack and locals size
        this.jasminCode.append(tabs).append(".limit stack 99\n")
                .append(tabs).append(".limit locals 99\n\n");

        // body
        for (Instruction i : method.getInstructions()) {
            this.instructionJasmin(tabs, i);
        }

        this.jasminCode.append(".end method\n");
    }

    public void instructionJasmin(String tabs, Instruction instr) {
        // for DEBUG
        //this.injectComment(tabs, instr.getInstType().toString());

        switch (instr.getInstType()) {
            case ASSIGN:
                this.assignInstructionJasmin(tabs, (AssignInstruction) instr);
                break;
            case CALL:
                this.callInstructionJasmin(tabs, (CallInstruction) instr);
                break;
            case GOTO:
                // this.gotoInstructionJasmin(tabs, (GotoInstruction) instr.;
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
                this.unOpInstructionJasmin(tabs, (UnaryOpInstruction) instr);
                break;
            case BINARYOPER:
                this.binOpInstructionJasmin(tabs, (BinaryOpInstruction) instr);
                break;
            case NOPER:
                this.noperInstructionJasmin(tabs, (SingleOpInstruction) instr);
                break;
            default:
                break;
        }
    }

    private String intLiteralPush(int i) {
        if (i == -1) {
            return "iconst_m1";
        } else if (i >= 0 && i <= 5) {
            return "iconst_" + i;
        } else if (i >= -128 && i <= 127) {
            return "bipush " + i;
        } else if (i >= -32768 && i <= 32767) {
            return "sipush " + i;
        } else {
            return "ldc " + i;
        }
    }

    private String boolLiteralPush(boolean b) {
        if (b)
            return "iconst_1";
        else
            return "iconst_0";
    }

    private String boolLiteralPush(int i) {
        return this.boolLiteralPush(i == 1);
    }

    private String loadCallArgLiteral(LiteralElement arg) {
        String literal = arg.getLiteral();
        String argStr;
        switch (arg.getType().getTypeOfElement()) {
            case INT32:
                argStr = this.intLiteralPush(Integer.parseInt(literal));
                break;
            case BOOLEAN:
                if (literal.equals("1"))
                    argStr = "iconst_1";
                else
                    argStr = "iconst_0";
                break;
            case OBJECTREF:
                Descriptor d = this.methodVarTable.get(literal);
                argStr = "aload_" + d.getVirtualReg();
                break;
            case STRING:
                argStr = "ldc " + literal;
                break;
            case THIS:
            case ARRAYREF:
            case CLASS:
            default:
                // pls
                return null;
        }

        return argStr;
    }

    private String getLoadInstr(String pre, int vReg) {
        if (vReg >= 0 && vReg <= 3)
            return pre + "load_" + vReg;
        else
            return pre + "load " + vReg;
    }

    private String loadCallArgOperand(Operand arg) {
        String name = arg.getName();
        String argStr;
        switch (arg.getType().getTypeOfElement()) {
            case INT32:
            case BOOLEAN:
                argStr = this.getLoadInstr("i", this.methodVarTable.get(name).getVirtualReg());
                break;
            case OBJECTREF:
                argStr = this.getLoadInstr("a", this.methodVarTable.get(name).getVirtualReg());
                break;
            case THIS:
                argStr = "aload_0";
                break;
            case STRING:
            case ARRAYREF:
            case CLASS:
            default:
                // pls
                return null;
        }

        return argStr;
    }

    private String loadCallArg(Element arg) {
        if (arg.isLiteral())
            return this.loadCallArgLiteral((LiteralElement) arg);
        else
            return this.loadCallArgOperand((Operand) arg);
    }

    private void loadCallArg(String tabs, Element arg) {
        String argStr = this.loadCallArg(arg);
        if (argStr != null)
            this.jasminCode.append(tabs).append(argStr).append("\n");
    }

    private String callArg(Element e) {
        if (e.isLiteral()) { // if the e is not a literal, then it is a variable
            return ((LiteralElement) e).getLiteral().replace("\"", "");
        } else {
            Operand o = (Operand) e;
            Type t = o.getType();
            switch (t.getTypeOfElement()) {
                case OBJECTREF:
                    return ((ClassType) t).getName();
                case THIS:
                    return this.ollirClass.getClassName();
                default:
                    return o.getName();
            }
        }
    }

    private void callInstructionJasmin(String tabs, CallInstruction instr) {
        StringBuilder ret = new StringBuilder().append(tabs);

        CallType invType = OllirUtils.getCallInvocationType(instr);
        switch (invType) {
            case invokevirtual:
                ret.append("invokevirtual ");
                this.loadCallArg(tabs, instr.getFirstArg());
                ret.append(this.callArg(instr.getFirstArg()));
                break;
            case invokeinterface:
                ret.append("invokeinterface ");
                this.loadCallArg(tabs, instr.getFirstArg());
                ret.append(this.callArg(instr.getFirstArg()));
                break;
            case invokespecial:
                ret.append("invokespecial ");
                ret.append(this.callArg(instr.getFirstArg()));
                break;
            case invokestatic:
                ret.append("invokestatic ");
                this.loadCallArg(tabs, instr.getFirstArg());
                ret.append(this.callArg(instr.getFirstArg()));
                break;
            case NEW:
                ret.append("new ");
                ret.append(this.callArg(instr.getFirstArg()));
                break;
            case arraylength:
                break;
            case ldc:
                break;
        }

        if (instr.getNumOperands() > 1) {
            if (invType != CallType.NEW) { // only new type instructions do not have a field with second arg
                ret.append(".").append(this.callArg(instr.getSecondArg()));
            }

            // args
            this.contextStack.push(instr);
            ret.append("(");
            for (Element arg : instr.getListOfOperands()) {
                this.loadCallArg(tabs, arg);
                ret.append(this.elemTypeJasmin(arg.getType()));
            }
            ret.append(")");
            this.contextStack.pop();
        }

        String returnStr = this.elemTypeJasmin(instr.getReturnType());
        this.jasminCode.append(ret).append(returnStr).append("\n");

        // post processing
        // call pop when the method return value should be ignored (not assign/calc and not void)
        if (this.contextStack.empty() && !returnStr.isEmpty() && !returnStr.equals("V"))
            this.jasminCode.append(tabs).append("pop\n");

        if (invType == CallType.NEW)
            this.jasminCode.append(tabs).append("dup\n");
    }

    private void retInstructionJasmin(String tabs, ReturnInstruction instr) {
        if (instr.hasReturnValue()) {
            this.loadCallArg(tabs, instr.getOperand());
            this.jasminCode.append(tabs).append(this.instrPreJasmin(instr.getOperand().getType()));
        } else {
            this.jasminCode.append(tabs);
        }

        this.jasminCode.append("return\n");
    }

    private void unOpInstructionJasmin(String tabs, UnaryOpInstruction instr) {
        this.contextStack.push(instr);
        Element elem = instr.getRightOperand();

        Operation op = instr.getUnaryOperation();
        switch (op.getOpType()) {
            case NOT:
                if (elem.isLiteral()) {
                    String boolLiteral = this.callArg(elem);
                    this.jasminCode.append(tabs)
                            .append(this.boolLiteralPush(Integer.parseInt(boolLiteral)))
                            .append("\n");
                } else {
                    // TODO
                }
                break;
            default:
                break;
        }
        this.contextStack.pop();
    }

    private void binOpInstructionJasmin(String tabs, BinaryOpInstruction instr) {
        this.contextStack.push(instr);

        Element leftElem = instr.getLeftOperand();
        Element rightElem = instr.getRightOperand();
        boolean bothLiteral = leftElem.isLiteral() && rightElem.isLiteral();

        Operation op = instr.getUnaryOperation();
        // for DEBUG
        //this.injectComment(tabs, op.getOpType().toString());
        switch (op.getOpType()) {
            case ADD:
                if (bothLiteral) {
                    String lhs = this.callArg(leftElem);
                    String rhs = this.callArg(rightElem);
                    this.jasminCode.append(tabs)
                            .append(this.intLiteralPush(Integer.parseInt(lhs) + Integer.parseInt(rhs)))
                            .append("\n");
                } else {
                    this.loadCallArg(tabs, leftElem);
                    this.loadCallArg(tabs, rightElem);
                    this.jasminCode.append(tabs).append("iadd\n");
                }
                break;
            case SUB:
                if (bothLiteral) {
                    String lhs = this.callArg(leftElem);
                    String rhs = this.callArg(rightElem);
                    this.jasminCode.append(tabs)
                            .append(this.intLiteralPush(Integer.parseInt(lhs) - Integer.parseInt(rhs)))
                            .append("\n");
                } else {
                    this.loadCallArg(tabs, leftElem);
                    this.loadCallArg(tabs, rightElem);
                    this.jasminCode.append(tabs).append("isub\n");
                }
                break;
            case MUL:
                if (bothLiteral) {
                    String lhs = this.callArg(leftElem);
                    String rhs = this.callArg(rightElem);
                    this.jasminCode.append(tabs)
                            .append(this.intLiteralPush(Integer.parseInt(lhs) * Integer.parseInt(rhs)))
                            .append("\n");
                } else {
                    this.loadCallArg(tabs, leftElem);
                    this.loadCallArg(tabs, rightElem);
                    this.jasminCode.append(tabs).append("imul\n");
                }
                break;
            case DIV:
                if (bothLiteral) {
                    String lhs = this.callArg(leftElem);
                    String rhs = this.callArg(rightElem);
                    this.jasminCode.append(tabs)
                            .append(this.intLiteralPush(Integer.parseInt(lhs) / Integer.parseInt(rhs)))
                            .append("\n");
                } else {
                    this.loadCallArg(tabs, leftElem);
                    this.loadCallArg(tabs, rightElem);
                    this.jasminCode.append(tabs).append("idiv\n");
                }
                break;
            case NOT:
                break;
            default:
                // TODO the other operations
                break;
        }

        this.contextStack.pop();
    }

    private void noperInstructionJasmin(String tabs, SingleOpInstruction instr) {
        this.loadCallArg(tabs, instr.getSingleOperand());
    }

    private String getStoreInstr(String pre, int vReg) {
        if (vReg >= 0 && vReg <= 3)
            return pre + "store_" + vReg;
        else
            return pre + "store " + vReg;
    }

    private void assignInstructionJasmin(String tabs, AssignInstruction instr) {
        this.contextStack.push(instr);
        this.instructionJasmin(tabs, instr.getRhs());
        this.contextStack.pop();

        Element dest = instr.getDest();
        Descriptor d = this.methodVarTable.get(((Operand) dest).getName());
        switch (dest.getType().getTypeOfElement()) {
            case INT32:
            case BOOLEAN:
                this.jasminCode.append(tabs)
                        .append(this.getStoreInstr("i", d.getVirtualReg()))
                        .append("\n");
                break;
            default: // assume it is a
                this.jasminCode.append(tabs)
                        .append(this.getStoreInstr("a", d.getVirtualReg()))
                        .append("\n");
                break;
        }
    }
}
