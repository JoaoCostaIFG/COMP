import GraphViewer.Vertex;
import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.*;

public class JasminEmitter {
    private static final String labelPrefix = "l";
    private static final boolean debug = true;

    private final ClassUnit ollirClass;
    private final List<Report> reports;
    private StringBuilder jasminCode;
    private Map<String, Descriptor> methodVarTable;
    private Map<String, Instruction> methodLabels;
    private final Stack<Instruction> contextStack;
    private Integer lineNo;
    private int stackSize, stackSizeCnt;
    private final Set<String> locals;

    public JasminEmitter(ClassUnit ollirClass, List<Report> reports) {
        this.ollirClass = ollirClass;
        this.reports = reports;
        this.jasminCode = new StringBuilder();
        this.methodVarTable = new HashMap<>();
        this.methodLabels = new HashMap<>();
        this.contextStack = new Stack<>();
        this.lineNo = 0;
        this.stackSize = this.stackSizeCnt = 0;
        this.locals = new HashSet<>();
    }

    public String getJasminCode() {
        return this.jasminCode.toString();
    }

    private JasminEmitter addEmptyLine() {
        this.jasminCode.append("\n");
        ++this.lineNo;
        return this;
    }

    private JasminEmitter addCode(String... args) {
        for (String a : args)
            this.jasminCode.append(a);
        return this;
    }

    private JasminEmitter addCodeLine(String... args) {
        for (String a : args)
            this.jasminCode.append(a);
        this.jasminCode.append("\n");
        ++this.lineNo;
        return this;
    }

    private JasminEmitter addCodeLine(StringBuilder builder) {
        this.jasminCode.append(builder).append("\n");
        ++this.lineNo;
        return this;
    }

    private JasminEmitter comment(String tabs, String... comments) {
        if (debug) {
            this.jasminCode.append(tabs).append(";");
            for (String c : comments)
                this.jasminCode.append(" ").append(c);
            this.jasminCode.append("\n");
            ++this.lineNo;
        }
        return this;
    }

    private JasminEmitter addLabel(String label) {
        this.jasminCode.append(label).append(":\n");
        ++this.lineNo;
        return this;
    }

    private JasminEmitter addLabeledCodeLine(String label, String tabs, String... args) {
        this.addLabel(label);
        this.jasminCode.append(tabs);
        for (String a : args)
            this.jasminCode.append(a);
        this.jasminCode.append("\n");
        ++this.lineNo;
        return this;
    }

    private String elemTypeJasmin(ElementType type) {
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

    private String elemTypeJasmin(Type type) {
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

    private String instrPreJasmin(Type type) {
        switch (type.getTypeOfElement()) {
            case INT32:
            case BOOLEAN:
                return "i";
            case VOID:
                return "";
            default:
                return "a";
        }
    }

    private StringBuilder setStringBuilder(StringBuilder sb) {
        StringBuilder prevSB = this.jasminCode;
        this.jasminCode = sb;
        return prevSB;
    }

    public String parse() {
        this.comment("", "Class accepted by Jasmin2.3")
                .addCodeLine(".class public ", this.ollirClass.getClassName())
                .addCode(".super ")
                .addCodeLine(Objects.requireNonNullElse(this.ollirClass.getSuperClass(), "java/lang/Object"));

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
            this.methodVarTable = method.getVarTable();
            this.methodLabels = method.getLabels();
            this.methodJasmin(method);
        }

        return this.getJasminCode();
    }

    private void standardInitializerJasmin() {
        String tabs = "\t";
        this.addEmptyLine()
                .comment("", "standard initializer")
                .addCodeLine(".method public <init>()V")
                .addCodeLine(tabs, "aload_0")
                .addCode(tabs, "invokespecial ")
                .addCode(Objects.requireNonNullElse(this.ollirClass.getSuperClass(), "java/lang/Object"))
                .addCodeLine(".<init>()V")
                .addCodeLine(tabs, "return")
                .addCodeLine(".end method");
    }

    private void updateStackSize(int toConsume) {
        if (this.stackSizeCnt > this.stackSize)
            this.stackSize = this.stackSizeCnt;
        if (toConsume < 0)
            this.stackSizeCnt = 0;
        else
            this.stackSizeCnt -= toConsume;
    }

    private void updateStackSize() {
        this.updateStackSize(-1);
    }

    private void methodJasmin(Method method) {
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
        // make the methods write on a temporary buffer so we can set the number of local vars and stack size
        StringBuilder bodyJasmin = new StringBuilder();
        StringBuilder classJasmin = this.setStringBuilder(bodyJasmin);

        // local vars: this + method args + local vars
        int maxRegisters = 0;
        this.locals.clear();
        if (!method.isStaticMethod()) this.locals.add("this");
        for (var e : this.methodVarTable.entrySet()) {
            this.locals.add(e.getKey());
        }

        int regsUsed = 0;
        if (maxRegisters > 0) {
            RegisterAllocator registerAllocator = new RegisterAllocator(method);
            regsUsed = registerAllocator.allocate(maxRegisters);
            System.out.println(regsUsed);
            if (regsUsed < 0) {
                this.reports.add(new Report(ReportType.ERROR, Stage.GENERATION, -1,
                        "It's not possible to limit the method " + method.getMethodName()
                                + " to " + maxRegisters + " register(s)."));
                return;
            }
            System.out.println(registerAllocator.getGraph());

            for (RegisterAllocatorIntruction v : registerAllocator.getInstructions()) {
                for (String varName : v.getDef()) {
                    this.locals.remove(varName);
                }
            }
        }

        // track the stack size limit value to set
        this.stackSize = 0;
        // body
        for (Instruction i : method.getInstructions()) {
            this.stackSizeCnt = 0;
            this.instructionJasmin(tabs, i);
            // update max stackSize (if needed)
            this.updateStackSize();
        }
        this.setStringBuilder(classJasmin);

        // stack and locals size
        this.addCodeLine(tabs, ".limit stack ", String.valueOf(this.stackSize))
                .addCodeLine(tabs, ".limit locals ", String.valueOf(this.locals.size() + regsUsed))
                .addEmptyLine();

        this.jasminCode.append(bodyJasmin);
        this.addCodeLine(".end method");
    }

    private void instructionJasmin(String tabs, Instruction instr) {
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
        // IMP this negates the given argument
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
                argStr = this.getLoadInstr("a", this.methodVarTable.get(literal).getVirtualReg());
                break;
            case STRING:
                argStr = "ldc " + literal;
                break;
            case ARRAYREF:
            case THIS:
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
                String pre = "i";
                if (arg.getClass().equals(ArrayOperand.class)) pre = "a";
                argStr = this.getLoadInstr(pre, this.methodVarTable.get(name).getVirtualReg());
                break;
            case OBJECTREF:
            case ARRAYREF:
            case STRING:
                argStr = this.getLoadInstr("a", this.methodVarTable.get(name).getVirtualReg());
                break;
            case THIS:
                argStr = "aload_0";
                break;
            case CLASS:
            default:
                // pls
                return null;
        }

        return argStr;
    }

    private String loadCallArg(Element arg) {
        String ret;
        if (arg.isLiteral()) {
            ret = this.loadCallArgLiteral((LiteralElement) arg);
        } else {
            ret = this.loadCallArgOperand((Operand) arg);
        }

        if (ret != null)
            ++this.stackSizeCnt;
        return ret;
    }

    private void loadCallArg(String tabs, Element arg) {
        String argStr = this.loadCallArg(arg);

        if (argStr != null) {
            this.addCodeLine(tabs, argStr);

            if (arg.getClass().equals(ArrayOperand.class)) {
                for (Element indexElem : ((ArrayOperand) arg).getIndexOperands())
                    this.loadCallArg(tabs, indexElem);

                switch (arg.getType().getTypeOfElement()) {
                    case INT32:
                    case BOOLEAN:
                        this.addCodeLine(tabs, "iaload");
                        break;
                    default:
                        this.addCodeLine(tabs, "aaload");
                        break;
                }
            }
        }
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

        CallType invType = instr.getInvocationType();
        this.comment(tabs, invType.toString()); // for DEBUG
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
                    this.updateStackSize(1);
                    ++this.stackSizeCnt;
                    return;
                } else {
                    ret.append("new ");
                    ret.append(this.callArg(instr.getFirstArg()));
                    ++this.stackSizeCnt;
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
        if (this.contextStack.empty() && !returnStr.isEmpty() && !returnStr.equals("V")) {
            this.addCodeLine(tabs, "pop");
            this.updateStackSize(1);
        }

        if (invType == CallType.NEW) {
            this.addCodeLine(tabs, "dup");
            ++this.stackSizeCnt;
        }
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
                this.updateStackSize(1);
                this.loadCallArg(tabs, rightElem);
                this.addCodeLine(tabs, "ifeq ", label);
                this.updateStackSize(1);
                ++this.stackSizeCnt;
                break;
            case ORB:
                // TODO need extra label if's
                break;
            case LTH:
                this.loadCallArg(tabs, leftElem);
                this.loadCallArg(tabs, rightElem);
                this.addCodeLine(tabs, "if_icmpge ", label);
                this.updateStackSize(2);
                ++this.stackSizeCnt;
                break;
            case GTH:
                this.loadCallArg(tabs, leftElem);
                this.loadCallArg(tabs, rightElem);
                this.addCodeLine(tabs, "if_icmple ", label);
                this.updateStackSize(2);
                ++this.stackSizeCnt;
                break;
            case LTE:
                this.loadCallArg(tabs, leftElem);
                this.loadCallArg(tabs, rightElem);
                this.addCodeLine(tabs, "if_icmpgt ", label);
                this.updateStackSize(2);
                ++this.stackSizeCnt;
                break;
            case GTE:
                this.loadCallArg(tabs, leftElem);
                this.loadCallArg(tabs, rightElem);
                this.addCodeLine(tabs, "if_icmplt ", label);
                this.updateStackSize(2);
                ++this.stackSizeCnt;
                break;
            case EQ:
                this.loadCallArg(tabs, leftElem);
                this.loadCallArg(tabs, rightElem);
                this.addCodeLine(tabs, "if_icmpne ", label);
                this.updateStackSize(2);
                ++this.stackSizeCnt;
                break;
            case NEQ:
                this.loadCallArg(tabs, leftElem);
                this.loadCallArg(tabs, rightElem);
                this.addCodeLine(tabs, "if_icmpeq ", label);
                this.updateStackSize(2);
                ++this.stackSizeCnt;
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
                ++this.stackSizeCnt;
            } else {
                // invert stored boolean value (if 0 => 1, if 1 => 0)
                this.loadCallArg(tabs, elem);
                // branch labels
                String elseLabel = JasminEmitter.labelPrefix + (this.lineNo + 4);
                String endLabel = JasminEmitter.labelPrefix + (this.lineNo + 5);
                this.addCodeLine(tabs, "ifne ", elseLabel)
                        .addCodeLine(tabs, "iconst_1")
                        .addCodeLine(tabs, "goto ", endLabel)
                        .addLabeledCodeLine(elseLabel, tabs, "iconst_0")
                        .addLabel(endLabel);
                ++this.stackSizeCnt;
            }
        }
        this.contextStack.pop();
    }

    private void intArithmetic(String tabs, Element leftElem, Element rightElem, String op) {
        if (leftElem.isLiteral() && rightElem.isLiteral()) { // bothLiteral
            String lhs = this.callArg(leftElem);
            String rhs = this.callArg(rightElem);
            int il = Integer.parseInt(lhs), ir = Integer.parseInt(rhs);
            int arg;
            switch (op) {
                case "iadd":
                    arg = il + ir;
                    break;
                case "isub":
                    arg = il - ir;
                    break;
                case "imul":
                    arg = il * ir;
                    break;
                case "idiv":
                    arg = il / ir;
                    break;
                default:
                    // unreachable
                    return;
            }
            this.addCodeLine(tabs, this.intLiteralPush(arg));
            ++this.stackSizeCnt;
        } else {
            this.loadCallArg(tabs, leftElem);
            this.loadCallArg(tabs, rightElem);
            this.addCodeLine(tabs, op);
            this.updateStackSize(2);
            ++this.stackSizeCnt;
        }
    }

    private void booleanArithmetic(String tabs, Element leftElem, Element rightElem, String op) {
        this.loadCallArg(tabs, leftElem);
        this.loadCallArg(tabs, rightElem);
        // branch labels
        String elseLabel = JasminEmitter.labelPrefix + (this.lineNo + 4);
        String endLabel = JasminEmitter.labelPrefix + (this.lineNo + 5);
        this.addCodeLine(tabs, op, " ", elseLabel)
                .addCodeLine(tabs, "iconst_1")
                .addCodeLine(tabs, "goto ", endLabel)
                .addLabeledCodeLine(elseLabel, tabs, "iconst_0")
                .addLabel(endLabel);

        this.updateStackSize(2);
        ++this.stackSizeCnt;
    }

    private void andBooleanArithmetic(String tabs, Element leftElem, Element rightElem) {
        // branch labels
        String elseLabel = JasminEmitter.labelPrefix + (this.lineNo + 7);
        String endLabel = JasminEmitter.labelPrefix + (this.lineNo + 8);

        this.loadCallArg(tabs, leftElem);
        this.addCodeLine(tabs, "ifeq ", elseLabel);
        this.updateStackSize(1);
        this.loadCallArg(tabs, rightElem);
        this.addCodeLine(tabs, "ifeq ", elseLabel);
        this.updateStackSize(1);

        this.addCodeLine(tabs, "iconst_1")
                .addCodeLine(tabs, "goto ", endLabel)
                .addLabeledCodeLine(elseLabel, tabs, "iconst_0")
                .addLabel(endLabel);

        ++this.stackSizeCnt;
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
            case ANDB:
                this.andBooleanArithmetic(tabs, leftElem, rightElem);
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
        Element dest = instr.getDest();
        Descriptor d = this.methodVarTable.get(((Operand) dest).getName());

        boolean isArrayAccess = dest.getClass().equals(ArrayOperand.class);
        if (isArrayAccess) {
            this.addCodeLine(tabs, this.getLoadInstr("a", d.getVirtualReg()));
            ++this.stackSizeCnt;

            for (Element indexElem : ((ArrayOperand) dest).getIndexOperands())
                this.loadCallArg(tabs, indexElem);
        }

        this.contextStack.push(instr);
        this.instructionJasmin(tabs, instr.getRhs());
        this.contextStack.pop();

        switch (dest.getType().getTypeOfElement()) {
            case INT32:
            case BOOLEAN:
                if (isArrayAccess)
                    this.addCodeLine(tabs, "iastore");
                else
                    this.addCodeLine(tabs, this.getStoreInstr("i", d.getVirtualReg()));
                break;
            default: // assume it is 'a'
                if (isArrayAccess)
                    this.addCodeLine(tabs, "aastore");
                else
                    this.addCodeLine(tabs, this.getStoreInstr("a", d.getVirtualReg()));
                break;
        }
    }
}
