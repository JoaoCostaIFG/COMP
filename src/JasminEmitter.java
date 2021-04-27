import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.ollir.OllirUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class JasminEmitter {
    private final boolean debug = true;

    private final ClassUnit ollirClass;
    private final StringBuilder jasminCode;
    private HashMap<String, Descriptor> methodVarTable;
    private HashMap<String, Instruction> methodLabels;
    private final Stack<Instruction> contextStack;
    private Integer lineNo;

    // TODO arrays
    // TODO new array
    // TODO array length
    // TODO stack and locals size

    public JasminEmitter(ClassUnit ollirClass) {
        this.ollirClass = ollirClass;
        this.jasminCode = new StringBuilder();
        this.methodVarTable = new HashMap<>();
        this.methodLabels = new HashMap<>();
        this.contextStack = new Stack<>();
        this.lineNo = 0;
    }

    public String getJasminCode() {
        return this.jasminCode.toString();
    }

    public JasminEmitter addEmptyLine() {
        this.jasminCode.append("\n");
        ++this.lineNo;
        return this;
    }

    public JasminEmitter addCode(String... args) {
        for (String a : args)
            this.jasminCode.append(a);
        return this;
    }

    public JasminEmitter addCodeLine(String... args) {
        for (String a : args)
            this.jasminCode.append(a);
        this.jasminCode.append("\n");
        ++this.lineNo;
        return this;
    }

    public JasminEmitter addCodeLine(StringBuilder builder) {
        this.jasminCode.append(builder).append("\n");
        ++this.lineNo;
        return this;
    }

    public JasminEmitter comment(String tabs, String... comments) {
        if (debug) {
            this.jasminCode.append(tabs).append(";");
            for (String c : comments)
                this.jasminCode.append(" ").append(c);
            this.jasminCode.append("\n");
            ++this.lineNo;
        }
        return this;
    }

    public JasminEmitter addLabel(String label) {
        this.jasminCode.append(label).append(":\n");
        ++this.lineNo;
        return this;
    }

    public JasminEmitter addLabeledCodeLine(String label, String tabs, String... args) {
        this.addLabel(label);
        this.jasminCode.append(tabs);
        for (String a : args)
            this.jasminCode.append(a);
        this.jasminCode.append("\n");
        ++this.lineNo;
        return this;
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
        this.comment("", "Class accepted by Jasmin2.3")
                .addCodeLine(".class public ", this.ollirClass.getClassName())
                .addCodeLine(".super java/lang/Object");

        // many assumptions are made here (could be a lot more complete)
        if (!this.ollirClass.getFields().isEmpty()) {
            this.addEmptyLine();
            for (Field field : this.ollirClass.getFields()) {
                this.addCodeLine(".field ",
                        field.getFieldAccessModifier().toString().toLowerCase(), " ",
                        field.getFieldName(), " ",
                        this.elemTypeJasmin(field.getFieldType()));
            }
        }

        for (Method method : this.ollirClass.getMethods()) {
            this.methodVarTable = OllirAccesser.getVarTable(method);
            this.methodLabels = OllirAccesser.getLabels(method);
            this.methodJasmin(method);
        }

        return this.getJasminCode();
    }

    public void standardInitializerJasmin() {
        String tabs = "\t";
        this.addEmptyLine()
                .comment("", "standard initializer")
                .addCodeLine(".method public <init>()V")
                .addCodeLine(tabs, "aload_0")
                .addCodeLine(tabs, "invokespecial java/lang/Object.<init>()V")
                .addCodeLine(tabs, "return")
                .addCodeLine(".end method");
    }

    public void methodJasmin(Method method) {
        if (method.isConstructMethod()) {
            this.standardInitializerJasmin();
            return;
        }

        this.addEmptyLine()
                .addCode(".method public ");
        if (method.isStaticMethod()) this.addCode("static ");
        this.addCode(method.getMethodName());

        // method signature (args)
        this.addCode("(");
        for (Element e : method.getParams()) {
            this.addCode(this.elemTypeJasmin(e.getType()));
        }
        this.addCode(")");

        Type retType = method.getReturnType();
        this.addCodeLine(this.elemTypeJasmin(retType));

        String tabs = "\t";
        // stack and locals size
        this.addCodeLine(tabs, ".limit stack 99")
                .addCodeLine(tabs, ".limit locals 99")
                .addEmptyLine();

        // body
        for (Instruction i : method.getInstructions()) {
            this.instructionJasmin(tabs, i);
        }

        this.addCodeLine(".end method");
    }

    public void instructionJasmin(String tabs, Instruction instr) {
        // for DEBUG
        this.comment(tabs, instr.getInstType().toString());

        // tag line (if any)
        for (Map.Entry<String, Instruction> e : this.methodLabels.entrySet()) {
            if (e.getValue().equals(instr))
                this.addLabel(e.getKey());
        }

        switch (instr.getInstType()) {
            case ASSIGN:
                this.assignInstructionJasmin(tabs, (AssignInstruction) instr);
                break;
            case CALL:
                this.callInstructionJasmin(tabs, (CallInstruction) instr);
                break;
            case GOTO:
                this.gotoInstructionJasmin(tabs, (GotoInstruction) instr);
                break;
            case BRANCH:
                this.branchInstructionJasmin(tabs, (CondBranchInstruction) instr);
                break;
            case RETURN:
                this.retInstructionJasmin(tabs, (ReturnInstruction) instr);
                break;
            case PUTFIELD:
                this.putfieldInstructionJasmin(tabs, (PutFieldInstruction) instr);
                break;
            case GETFIELD:
                this.getfieldInstructionJasmin(tabs, (GetFieldInstruction) instr);
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
        // this negates the given argument
        return this.boolLiteralPush(i == 0);
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
            case ARRAYREF:
                argStr = this.getLoadInstr("a", this.methodVarTable.get(name).getVirtualReg());
                break;
            case THIS:
                argStr = "aload_0";
                break;
            case STRING:
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
            this.addCodeLine(tabs, argStr);
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
        this.comment(tabs, invType.toString()); // fr DEBUG
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
                if (this.callArg(instr.getFirstArg()).equals("array")) {
                    // arrays are weird.
                    this.loadCallArg(tabs, instr.getListOfOperands().get(0));
                    this.addCodeLine(tabs, "newarray int");
                    return;
                } else {
                    ret.append("new ");
                }
                break;
            case arraylength:
                // arrays are weird pt.2
                this.loadCallArg(tabs, instr.getFirstArg());
                this.addCodeLine(tabs, "arraylength");
                return;
            default:
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
        ret.append(returnStr);
        this.addCodeLine(ret);

        // post processing
        // call pop when the method return value should be ignored (not assign/calc and not void)
        if (this.contextStack.empty() && !returnStr.isEmpty() && !returnStr.equals("V"))
            this.addCodeLine(tabs, "pop");

        if (invType == CallType.NEW)
            this.addCodeLine(tabs, "dup");
    }

    private void putfieldInstructionJasmin(String tabs, PutFieldInstruction instr) {
        Element firstElem = instr.getFirstOperand();
        Element secondElem = instr.getSecondOperand();
        Element thirdElem = instr.getThirdOperand();

        this.loadCallArg(tabs, firstElem);
        this.loadCallArg(tabs, thirdElem);
        this.addCodeLine(tabs,
                "putfield ",
                this.callArg(firstElem), ".",
                this.callArg(secondElem), " ",
                this.elemTypeJasmin(secondElem.getType()));
    }

    private void getfieldInstructionJasmin(String tabs, GetFieldInstruction instr) {
        Element firstElem = instr.getFirstOperand();
        Element secondElem = instr.getSecondOperand();

        this.loadCallArg(tabs, firstElem);
        this.addCodeLine(tabs,
                "getfield ",
                this.callArg(firstElem), ".",
                this.callArg(secondElem), " ",
                this.elemTypeJasmin(secondElem.getType()));
    }

    private void retInstructionJasmin(String tabs, ReturnInstruction instr) {
        if (instr.hasReturnValue()) {
            this.loadCallArg(tabs, instr.getOperand());
            this.addCode(tabs, this.instrPreJasmin(instr.getOperand().getType()));
        } else {
            this.addCode(tabs);
        }
        this.addCodeLine("return");
    }

    private void gotoInstructionJasmin(String tabs, GotoInstruction instr) {
        String label = instr.getLabel();
        this.addCodeLine(tabs, "goto ", label);
    }

    private void branchInstructionJasmin(String tabs, CondBranchInstruction instr) {
        Element leftElem = instr.getLeftOperand();
        Element rightElem = instr.getRightOperand();
        String label = instr.getLabel();

        Operation op = instr.getCondOperation();
        this.comment(tabs, op.getOpType().toString()); // for DEBUG
        switch (op.getOpType()) {
            case ANDB:
                this.loadCallArg(tabs, leftElem);
                this.addCodeLine(tabs, "ifeq ", label);
                this.loadCallArg(tabs, rightElem);
                this.addCodeLine(tabs, "ifeq ", label);
                break;
            case LTH:
                this.loadCallArg(tabs, leftElem);
                this.loadCallArg(tabs, rightElem);
                this.addCodeLine(tabs, "if_icmpge ", label);
                break;
            case GTH:
                this.loadCallArg(tabs, leftElem);
                this.loadCallArg(tabs, rightElem);
                this.addCodeLine(tabs, "if_icmple ", label);
                break;
            case LTE:
                this.loadCallArg(tabs, leftElem);
                this.loadCallArg(tabs, rightElem);
                this.addCodeLine(tabs, "if_icmpgt ", label);
                break;
            case GTE:
                this.loadCallArg(tabs, leftElem);
                this.loadCallArg(tabs, rightElem);
                this.addCodeLine(tabs, "if_icmplt ", label);
                break;
            case EQ:
                this.loadCallArg(tabs, leftElem);
                this.loadCallArg(tabs, rightElem);
                this.addCodeLine(tabs, "if_icmpne ", label);
                break;
            case NEQ:
                this.loadCallArg(tabs, leftElem);
                this.loadCallArg(tabs, rightElem);
                this.addCodeLine(tabs, "if_icmpeq ", label);
                break;
            default:
                break;
        }
    }

    private void unOpInstructionJasmin(String tabs, UnaryOpInstruction instr) {
        this.contextStack.push(instr);
        Element elem = instr.getRightOperand();

        Operation op = instr.getUnaryOperation();
        this.comment(tabs, op.getOpType().toString()); // for DEBUG
        // the only unary operation we know about is NOTB (!)
        if (op.getOpType() == OperationType.NOTB) {
            if (elem.isLiteral()) {
                String boolLiteral = this.callArg(elem);
                this.addCodeLine(tabs, this.boolLiteralPush(Integer.parseInt(boolLiteral)));
            } else {
                // invert stored boolean value (if 0 => 1, if 1 => 0)
                this.loadCallArg(tabs, elem);
                // branch labels
                String elseLabel = "l" + (this.lineNo + 4);
                String endLabel = "l" + (this.lineNo + 5);
                this.addCodeLine(tabs, "ifne ", elseLabel)
                        .addCodeLine(tabs, "iconst_1")
                        .addCodeLine(tabs, "goto ", endLabel)
                        .addLabeledCodeLine(elseLabel, tabs, "iconst_0")
                        .addLabel(endLabel);
            }
        }
        this.contextStack.pop();
    }

    private void intArithmetic(String tabs, Element leftElem, Element rightElem, String op) {
        if (leftElem.isLiteral() && rightElem.isLiteral()) { // bothLiteral
            String lhs = this.callArg(leftElem);
            String rhs = this.callArg(rightElem);
            this.addCodeLine(tabs, this.intLiteralPush(Integer.parseInt(lhs) + Integer.parseInt(rhs)));
        } else {
            this.loadCallArg(tabs, leftElem);
            this.loadCallArg(tabs, rightElem);
            this.addCodeLine(tabs, op);
        }
    }

    private void booleanArithmetic(String tabs, Element leftElem, Element rightElem, String op) {
        this.loadCallArg(tabs, leftElem);
        this.loadCallArg(tabs, rightElem);
        // branch labels
        String elseLabel = "l" + (this.lineNo + 4);
        String endLabel = "l" + (this.lineNo + 5);
        this.addCodeLine(tabs, op, " ", elseLabel)
                .addCodeLine(tabs, "iconst_1")
                .addCodeLine(tabs, "goto ", endLabel)
                .addLabeledCodeLine(elseLabel, tabs, "iconst_0")
                .addLabel(endLabel);
    }

    private void binOpInstructionJasmin(String tabs, BinaryOpInstruction instr) {
        this.contextStack.push(instr);

        Element leftElem = instr.getLeftOperand();
        Element rightElem = instr.getRightOperand();

        Operation op = instr.getUnaryOperation();
        this.comment(tabs, op.getOpType().toString()); // for DEBUG
        switch (op.getOpType()) {
            case ADD:
                this.intArithmetic(tabs, leftElem, rightElem, "iadd");
                break;
            case SUB:
                this.intArithmetic(tabs, leftElem, rightElem, "isub");
                break;
            case MUL:
                this.intArithmetic(tabs, leftElem, rightElem, "imul");
                break;
            case DIV:
                this.intArithmetic(tabs, leftElem, rightElem, "idiv");
                break;
            case LTH:
                this.booleanArithmetic(tabs, leftElem, rightElem, "if_icmpge");
                break;
            case GTH:
                this.booleanArithmetic(tabs, leftElem, rightElem, "if_icmple");
                break;
            case EQ:
                this.booleanArithmetic(tabs, leftElem, rightElem, "if_icmpne");
                break;
            case NEQ:
                this.booleanArithmetic(tabs, leftElem, rightElem, "if_icmpeq");
                break;
            case LTE:
                this.booleanArithmetic(tabs, leftElem, rightElem, "if_icmpgt");
                break;
            case GTE:
                this.booleanArithmetic(tabs, leftElem, rightElem, "if_icmplt");
                break;
            default:
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
                this.addCodeLine(tabs, this.getStoreInstr("i", d.getVirtualReg()));
                break;
            default: // assume it is a
                this.addCodeLine(tabs, this.getStoreInstr("a", d.getVirtualReg()));
                break;
        }
    }
}
