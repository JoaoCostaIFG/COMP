import GraphViewer.Graph;
import GraphViewer.Vertex;
import org.specs.comp.ollir.Instruction;
import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.NodeType;

import java.util.*;

public class RegisterAllocator {
    private final Method method;
    private final List<RegisterAllocatorIntruction> instructions;
    private Graph g;

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
        this.createGraph();
    }

    public List<RegisterAllocatorIntruction> getInstructions() {
        return instructions;
    }

    private void fillDefUseMap(Instruction instr) {
        if (this.instructions.get(instr.getId() - 1) != null)
            return;

        RegisterAllocatorIntruction rInstr = new RegisterAllocatorIntruction(this.method, instr);
        this.instructions.set(instr.getId() - 1, rInstr);
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
        }

        System.out.println(this);
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
        }

        System.out.println(this);
    }

    private boolean instrChanged() {
        for (RegisterAllocatorIntruction ri : this.instructions) {
            if (ri.changed())
                return true;
        }
        return false;
    }

    private void createGraph() {
        this.g = new Graph();
        for (RegisterAllocatorIntruction ri : this.instructions) {
            for (String info : ri.getDef()) {
                this.g.addVertex(info);
            }
        }

        for (RegisterAllocatorIntruction ri : this.instructions) {
            for (String origInfo : ri.getIn()) {
                for (String destInfo : ri.getIn()) {
                    this.g.addEdge(origInfo, destInfo);
                }
            }

            for (String origInfo : ri.getOut()) {
                for (String destInfo : ri.getOut()) {
                    this.g.addEdge(origInfo, destInfo);
                }
            }
        }
    }

    public int allocate(int maxRegNo) {
        if (!this.g.graphColoring(maxRegNo))
            return -1;

        // get regs available for local vars
        List<Integer> regsAvailable = new ArrayList<>();
        for (var entry : this.method.getVarTable().entrySet()) {
            Vertex v = this.g.getVertexByInfo(entry.getKey());
            if (v == null) continue;
            regsAvailable.add(entry.getValue().getVirtualReg());
        }
        Collections.sort(regsAvailable);

        // set regs for the local vars we found
        for (var entry : this.method.getVarTable().entrySet()) {
            Vertex v = this.g.getVertexByInfo(entry.getKey());
            if (v == null) continue;
            entry.getValue().setVirtualReg(regsAvailable.get(v.getColor()));
        }

        return this.g.getColorsUsed();
    }

    public Graph getGraph() {
        return g;
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
