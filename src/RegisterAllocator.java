import org.specs.comp.ollir.Instruction;
import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.NodeType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RegisterAllocator {
    private final Method method;
    private List<RegisterAllocatorIntruction> instructions;

    public RegisterAllocator(Method method) {
        this.method = method;
        this.instructions = new ArrayList<>();

        List<Instruction> methodInstructions = method.getInstructions();
        for (Instruction i : methodInstructions) {
            if (i.getNodeType() == NodeType.INSTRUCTION)
                this.instructions.add(null);
        }

        this.fillDefUseMap();
        this.livenessAnalysis();
    }

    private void fillDefUseMap(Instruction instr) {
        if (this.instructions.get(instr.getId() - 1) != null)
            return;

        RegisterAllocatorIntruction rInstr = new RegisterAllocatorIntruction(instr);
        this.instructions.set(instr.getId() - 1, rInstr);

        /*
        for (Node node : instr.getPred()) {
            if (node.getNodeType() == NodeType.INSTRUCTION)
                this.fillDefUseMap((Instruction) node);
        }
        */
    }

    private void fillDefUseMap() {
        for (Instruction i : this.method.getInstructions()) {
            this.fillDefUseMap(i);
        }
    }

    private void livenessAnalysis() {
        System.out.println("Liveness analysis");

        while (this.instrChanged()) {
            for (int i = this.instructions.size() - 1; i >= 0; --i) {
                RegisterAllocatorIntruction rInstr = this.instructions.get(i);

                // outs
                Set<String> newOut = new HashSet<>();
                for (int j : rInstr.getSuc())
                    newOut.addAll(this.instructions.get(j - 1).getIn());
                rInstr.setOut(newOut);

                // ins
                Set<String> newIn = new HashSet<>(rInstr.getOut());
                newIn.removeAll(rInstr.getDef());
                newIn.addAll(rInstr.getUse());
                rInstr.setIn(newIn);
            }

            System.out.println(this);
        }
    }

    private void livenessAnalysisForward() {
        System.out.println("Liveness analysis (forward)");

        while (this.instrChanged()) {
            for (int i = 0; i < this.instructions.size(); ++i) {
                RegisterAllocatorIntruction rInstr = this.instructions.get(i);

                // ins
                Set<String> newIn = new HashSet<>(rInstr.getOut());
                newIn.removeAll(rInstr.getDef());
                newIn.addAll(rInstr.getUse());
                rInstr.setIn(newIn);

                // outs
                Set<String> newOut = new HashSet<>();
                for (int j : rInstr.getSuc())
                    newOut.addAll(this.instructions.get(j - 1).getIn());
                rInstr.setOut(newOut);
            }

            System.out.println(this);
        }
    }

    private boolean instrChanged() {
        for (RegisterAllocatorIntruction ri : this.instructions) {
            if (ri.changed())
                return true;
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        for (RegisterAllocatorIntruction i : this.instructions) {
            builder.append(i).append("\n");
        }

        return builder.toString();
    }
}
