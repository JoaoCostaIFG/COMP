package Backend;

import Backend.GraphViewer.Graph;
import Backend.GraphViewer.Vertex;
import org.specs.comp.ollir.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RegisterAllocator {
    private final boolean debug;
    private final Method method;
    private final List<RegisterAllocatorIntruction> instructions;
    private Graph g;

    public RegisterAllocator(Method method, boolean debug) {
        this.debug = debug;
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

    public RegisterAllocator(Method method) {
        this(method, false);
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
        for (Instruction i : this.method.getInstructions())
            this.fillDefUseMap(i);
    }

    private void livenessAnalysis() {
        if (this.debug) System.out.println("Liveness analysis");

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

        if (this.debug) System.out.println(this);
    }

    private void livenessAnalysisForward() {
        if (this.debug) System.out.println("Liveness analysis (forward)");

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

        if (this.debug) System.out.println(this);
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
        // get variables that are locally defined (no arguments)
        for (RegisterAllocatorIntruction ri : this.instructions) {
            for (String info : ri.getDef()) {
                this.g.addVertex(info);
            }
        }

        // register conflicts
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

    public List<Integer> deadAssignments() {
        // This is mainly used for meaningless assignments caused by the combination of the constant optimization and
        // constant propagation optimizations combination. Assignments like a = 1; that aren't used anywhere in the code
        // are removed.

        List<Integer> ret = new ArrayList<>();
        for (RegisterAllocatorIntruction i : this.instructions) {
            if (i.getDef().size() == 0)
                continue;

            Instruction instruction = i.getInstr();
            if (instruction.getInstType() != InstructionType.ASSIGN)
                continue;
            AssignInstruction assignInstruction = (AssignInstruction) instruction;
            if (assignInstruction.getRhs().getInstType() != InstructionType.NOPER)
                continue;

            boolean contains = false;
            for (String def : i.getDef()) {
                if (i.getOut().contains(def)) {
                    contains = true;
                    break;
                }
            }
            if (!contains) ret.add(i.getId());
        }

        // remove dead instructions from allocation
        // IMP: this is not useful, because these kind of instruction will be by themselves in a graph. As such
        // these can't conflict with any other variables.
        // Note2: this is actually bad, because this wouldn't be removed fro mthe register count
        /*
        for (Integer i : ret) {
            this.instructions.removeIf((
                    RegisterAllocatorIntruction instr) -> instr.getId() == i);
        }
        */

        return ret;
    }

    public int allocate(int maxRegNo) {
        this.createGraph();

        if (!this.g.graphColoring(maxRegNo))
            return -1;

        int startInd = 0;
        if (!this.method.isStaticMethod()) ++startInd;
        startInd += this.method.getParams().size();

        int colorsUsed = this.g.getColorsUsed();
        int currInd = startInd + colorsUsed;
        for (var e : this.method.getVarTable().entrySet()) {
            Descriptor d = e.getValue();
            // skip this, method params, and class fields
            if (d.getVirtualReg() == -1 || d.getVirtualReg() < startInd)
                continue;

            Vertex v = this.g.getVertexByInfo(e.getKey());
            if (v == null)
                d.setVirtualReg(currInd++);
            else
                d.setVirtualReg(startInd + v.getColor());
        }

        System.out.println(method.getMethodName());
        System.out.print("\t");
        for (var e : method.getVarTable().entrySet())
            System.out.print(e.getKey() + ":" + e.getValue().getVirtualReg() + " ");
        System.out.println("\n");

        return currInd;
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
