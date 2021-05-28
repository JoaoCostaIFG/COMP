import org.specs.comp.ollir.Node;
import org.specs.comp.ollir.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RegisterAllocatorIntruction {
    private final Instruction instr;
    private final Set<String> def;
    private final Set<String> use;
    private Set<String> oldIn, in;
    private Set<String> oldOut, out;

    public RegisterAllocatorIntruction(Instruction instr) {
        this.instr = instr;
        this.def = new HashSet<>();
        this.use = new HashSet<>();
        this.oldIn = null;
        this.in = new HashSet<>();
        this.oldOut = null;
        this.out = new HashSet<>();

        this.fillDef();
    }

    private void fillDef() {
        if (this.instr.getInstType() == InstructionType.ASSIGN) {
            AssignInstruction assignInstruction = (AssignInstruction) instr;
            this.fillUsesMap(assignInstruction.getRhs());

            // TODO array assignment
            // TODO PUTFIELD
            Element dest = assignInstruction.getDest();
            if (dest.getClass().equals(ArrayOperand.class)) {
                ArrayOperand arrayOperand = (ArrayOperand) dest;
                for (Element indexElem : arrayOperand.getIndexOperands()) {
                    this.addOperandToUse(indexElem);
                }
            } else {
                String destName = this.getElemName(dest);
                if (destName != null) this.def.add(destName);
            }
        } else {
            this.fillUsesMap(instr);
        }

    }

    private void fillUsesMap(Instruction instr) {
        // TODO
        Element e;
        switch (instr.getInstType()) {
            case ASSIGN:
                AssignInstruction assignInstruction = (AssignInstruction) instr;
                // dest
                e = assignInstruction.getDest();
                if (e.getClass().equals(ArrayOperand.class)) {
                    ArrayOperand arrayOperand = (ArrayOperand) e;
                    for (Element indexElem : arrayOperand.getIndexOperands()) {
                        this.addOperandToUse(indexElem);
                    }
                }
                break;
            case BRANCH:
                CondBranchInstruction condBranchInstruction = (CondBranchInstruction) instr;
                this.addOperandToUse(condBranchInstruction.getLeftOperand());
                this.addOperandToUse(condBranchInstruction.getRightOperand());
                break;
            case CALL:
                CallInstruction callInstruction = (CallInstruction) instr;
                if (callInstruction.getInvocationType() != CallType.NEW) {
                    this.addOperandToUse(callInstruction.getFirstArg());
                    if (callInstruction.getNumOperands() > 1) {
                        this.addOperandToUse(callInstruction.getSecondArg());
                        for (Element arg : callInstruction.getListOfOperands())
                            this.addOperandToUse(arg);
                    }
                } else if (callInstruction.getListOfOperands().size() > 0) {
                    this.addOperandToUse(callInstruction.getListOfOperands().get(0));
                }
                break;
            case RETURN:
                ReturnInstruction returnInstruction = (ReturnInstruction) instr;
                if (returnInstruction.hasReturnValue())
                    this.addOperandToUse(returnInstruction.getOperand());
                break;
            case PUTFIELD:
                break;
            case GETFIELD:
                break;
            case UNARYOPER:
                UnaryOpInstruction unaryOpInstruction = (UnaryOpInstruction) instr;
                this.addOperandToUse(unaryOpInstruction.getRightOperand());
                break;
            case BINARYOPER:
                BinaryOpInstruction binaryOpInstruction = (BinaryOpInstruction) instr;
                this.addOperandToUse(binaryOpInstruction.getLeftOperand());
                this.addOperandToUse(binaryOpInstruction.getRightOperand());
                break;
            default:
                break;
        }
    }

    private String getElemName(Element e) {
        if (!e.getClass().equals(Operand.class)) return null;
        Operand o = (Operand) e;
        if (o.getType().getTypeOfElement() == ElementType.CLASS) return null;
        return o.getName();
    }

    private void addOperandToUse(Element e) {
        String name = this.getElemName(e);
        if (name != null) this.use.add(name);
    }


    public Set<String> getDef() {
        return def;
    }

    public Set<String> getUse() {
        return use;
    }

    public List<Integer> getSuc() {
        List<Integer> ret = new ArrayList<>();
        Node suc = this.instr.getSucc1();
        if (suc.getId() != 0)
            ret.add(suc.getId());
        suc = this.instr.getSucc2();
        if (suc != null && suc.getId() != 0)
            ret.add(suc.getId());
        return ret;
    }

    public Set<String> getIn() {
        return in;
    }

    public void setIn(Set<String> in) {
        this.oldIn = this.in;
        this.in = in;
    }

    public Set<String> getOut() {
        return out;
    }

    public void setOut(Set<String> out) {
        this.oldOut = this.out;
        this.out = out;
    }

    public boolean changed() {
        return !eqSet(this.oldIn, this.in) || !eqSet(this.oldOut, this.out);
    }

    private static boolean eqSet(Set<?> a, Set<?> b) {
        if (a == null || b == null)
            return false;
        if (a.size() != b.size())
            return false;
        return a.containsAll(b);
    }

    @Override
    public String toString() {
        return this.instr.getId() + " Defs: " + this.def + " Uses: " + this.use
                + " In: " + this.in + " Out: " + this.out;
    }
}
