import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.List;

import static java.lang.Integer.parseInt;

public class LocalVarsVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    private boolean success;
    private final MySymbolTable symbolTable;
    private final List<Symbol> methodParameters;
    private final List<Symbol> localVars;

    public LocalVarsVisitor(MySymbolTable symbolTable, List<Symbol> methodParameters, List<Symbol> localVars) {
        super();
        this.symbolTable = symbolTable;
        this.methodParameters = methodParameters;
        this.localVars = localVars;
        this.success = true;
        addVisit("VarDeclaration", this::parseLocalVars);
    }

    public boolean isSuccess() {
        return success;
    }

    private Boolean parseLocalVars(JmmNode node, List<Report> reports) {
        String varName = node.get("varName");
        // conflicts with class field
        // if (this.symbolTable.getField(varName) != null) {
        //     reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")),
        //             "Variable declaration " + varName + " conflicts with the class field with the same name."));
        //     this.success = false;
        //     return false;
        // }

        // conflicts with parameter
        if (this.methodParameters.stream().anyMatch(s -> s.getName().equals(varName))) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")),
                    "Variable declaration " + varName + " conflicts with the parameter with the same name."));
            this.success = false;
            return false;
        }

        // conflicts with another local var
        if (this.localVars.stream().anyMatch(s -> s.getName().equals(varName))) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")),
                    "Variable declaration " + varName + " conflicts with another local variable with the same name."));
            this.success = false;
            return false;
        }

        // type
        JmmNode typeNode = node.getChildren().get(0);
        if (!typeNode.getKind().equals("Type")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(typeNode.get("line")),
                    "Declaration of variable " + varName + " doesn't have a type."));
            this.success = false;
            return false;
        }
        Type type = new Type(typeNode.get("dataType"), typeNode.get("isArray").equals("yes"));

        this.localVars.add(new Symbol(type, varName));
        return true;
    }

    public List<Symbol> getLocalVars() {
        return localVars;
    }

    public List<Symbol> getMethodParameters() {
        return methodParameters;
    }
}
