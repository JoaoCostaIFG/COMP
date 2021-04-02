import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.List;

public class BodyVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    private final MySymbolTable symbolTable;

    public BodyVisitor(MySymbolTable symbolTable) {
        super();
        this.symbolTable = symbolTable;
        addVisit("Binary", this::visitBinary);
    }

    private Boolean visitBinary(JmmNode node, List<Report> reports) {
        System.err.println(node.get("op"));
        return true;
    }
}
