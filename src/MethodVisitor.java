import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Integer.parseInt;

public class MethodVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    private final MySymbolTable symbolTable;

    public MethodVisitor(MySymbolTable symbolTable) {
        super();
        this.symbolTable = symbolTable;
        addVisit("MethodDeclaration", this::parseMethodDeclaration);
    }

    // IMP local vars can't have the same name has method parameters

    // TODO have an explicit main visitor that we can easily separate everything?
    // TODO not that good of an idea, can just have 2 auxiliar methods
    private Boolean parseMethodDeclaration(JmmNode node, List<Report> reports) {
        String methodName = node.get("methodName");
        Type returnType;
        List<Symbol> methodParameters = new ArrayList<>();

        if (methodName.equals("main")) {  // is main
            returnType = new Type("void", false);
            JmmNode mainParam = node.getChildren().get(0);
            // TODO check for correct type
            methodParameters.add(new Symbol(new Type("String", true), mainParam.get("paramName")));
        } else {
            List<JmmNode> children = node.getChildren();
            if (children.size() != 3) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")),
                        "Method " + methodName + " isn't properly defined."));
                return false;
            }
            // TODO check if all 3 children are of the correct type
            // return type
            JmmNode returnNode = children.get(0);
            returnType = new Type(returnNode.get("dataType"), returnNode.get("isArray").equals("yes"));

            // parameters
            JmmNode parametersNode = children.get(1);
            for (JmmNode paramNode : parametersNode.getChildren()) {
                String paramName = paramNode.get("paramName");
                // parameter type
                JmmNode typeNode = paramNode.getChildren().get(0);
                boolean isArray = typeNode.get("isArray").equals("yes");
                if (isArray) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(typeNode.get("line")),
                            "Method " + methodName + " has an array parameter," + paramName + "."));
                    return false;
                }
                // we know parameters can't be arrays
                Type paramType = new Type(typeNode.get("dataType"), false);
                methodParameters.add(new Symbol(paramType, paramName));
            }

            // body
            JmmNode bodyNode = children.get(2);
        }

        // TODO call methodBodyVisitor here (body Node)
        // TODO symbol table should take the 4 args
        this.symbolTable.addMethod(methodName, returnType, methodParameters, new ArrayList<>());
        return true;
    }
}
