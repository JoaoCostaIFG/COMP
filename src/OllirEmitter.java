import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;

public class OllirEmitter extends PreorderJmmVisitor<Boolean, String> {
    private final SymbolTable symbolTable;
    private String ollirCode;

    public OllirEmitter(SymbolTable symbolTable) {
        super();
        this.symbolTable = symbolTable;
        ollirCode = "";
        this.addVisit("Program", this::visitRoot);
    }

    private String visitRoot(JmmNode node, Boolean ignored) {
        System.err.println(node);
        return this.ollirCode;
    }
}
