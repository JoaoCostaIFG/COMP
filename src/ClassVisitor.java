import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.List;

import static java.lang.Integer.parseInt;

public class ClassVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    private final MySymbolTable symbolTable;

    public ClassVisitor(MySymbolTable symbolTable) {
        super();
        this.symbolTable = symbolTable;
        addVisit("ClassDeclaration", this::parseClass);
    }

    private Boolean parseClass(JmmNode node, List<Report> reports) {
        if (!node.getKind().equals("ClassDeclaration"))
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    parseInt(node.get("line")), parseInt(node.get("col")),
                    "This should be a class declaration"));

        this.symbolTable.setClassName(node.get("className"));
        try {
            this.symbolTable.setSuper(node.get("extendsName"));
        } catch (Exception ignored) {
        }
        return true;
    }
}
