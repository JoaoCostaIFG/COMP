import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Integer.parseInt;

public class ImportVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    private final MySymbolTable symbolTable;

    public ImportVisitor(MySymbolTable symbolTable) {
        super();
        this.symbolTable = symbolTable;
        addVisit("ImportDeclarations", this::parseImports);
        // setDefaultVisit((jmmNode, reports) -> new ArrayList<>());
    }

    private Boolean parseImports(JmmNode node, List<Report> reports) {
        List<String> imports = new ArrayList<>();
        for (var child : node.getChildren()) {
            if (!child.getKind().equals("ImportDeclaration"))
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                        parseInt(child.get("line")), parseInt(child.get("col")),
                        "This should be an import declaration"));
            else
                imports.add(child.get("importPath"));
        }
        this.symbolTable.setImports(imports);
        return true;
    }
}
