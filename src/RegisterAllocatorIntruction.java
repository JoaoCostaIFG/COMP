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
            this.fillUsesMap(((AssignInstruction) instr).getRhs());

            // TODO array assignment
            // TODO PUTFIELD

            AssignInstruction assignInstruction = (AssignInstruction) instr;
            Element dest = assignInstruction.getDest();
            if (dest.getClass().equals(ArrayOperand.class)) {
                ArrayOperand arrayOperand = (ArrayOperand) dest;
                for (Element indexElem : arrayOperand.getIndexOperands()) {
                    String name = this.getElemName(indexElem);
                    if (name != null) this.use.add(name);
                }
            } else {
                String name = this.getElemName(dest);
                if (name != null) this.def.add(name);
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
                        String name = this.getElemName(indexElem);
                        if (name != null) this.use.add(name);
                    }
                }
                break;
            case CALL:
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
                UnaryOpInstruction unaryOpInstruction = (UnaryOpInstruction) instr;
                String name = this.getElemName(unaryOpInstruction.getRightOperand());
                if (name != null) this.use.add(name);
                break;
            case BINARYOPER:
                BinaryOpInstruction binaryOpInstruction = (BinaryOpInstruction) instr;
                name = this.getElemName(binaryOpInstruction.getLeftOperand());
                if (name != null) this.use.add(name);
                name = this.getElemName(binaryOpInstruction.getRightOperand());
                if (name != null) this.use.add(name);
                break;
            default:
                break;
        }
    }

    private String getElemName(Element e) {
        if (!e.getClass().equals(Operand.class))
            return null;

        return ((Operand) e).getName();
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

    @Override
    public String toString() {
        return this.instr.getId() + " Defs: " + this.def + " Uses: " + this.use
                + " In: " + this.in + " Out: " + this.out;
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
}
