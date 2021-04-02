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
    private static int mainCount = 0;
    private final MySymbolTable symbolTable;

    public MethodVisitor(MySymbolTable symbolTable) {
        super();
        this.symbolTable = symbolTable;
        addVisit("MethodDeclaration", this::parseMethodDeclaration);
    }

    // IMP local vars can't have the same name has method parameters
    // TODO we can get the Return node here but aren't!!!!!!!!!
    // TODO main needs to have extra checks

    private Boolean parseMethodDeclaration(JmmNode node, List<Report> reports) {
        String methodName = node.get("methodName");
        Type returnType;
        List<Symbol> methodParameters = new ArrayList<>();
        JmmNode bodyNode;

        if (methodName.equals("main")) {  // is main
            if (mainCount >= 1) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")),
                        "Redeclaration of main function."));
                return false;
            }
            ++mainCount;
            returnType = new Type("void", false);
            // return type
            JmmNode mainParam = node.getChildren().get(0);
            if (!mainParam.getKind().equals("MainParameter")) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(mainParam.get("line")),
                        "Main parameter name isn't defined."));
                return false;
            }
            // parameters
            methodParameters.add(new Symbol(new Type("String", true), mainParam.get("paramName")));
            // body node
            bodyNode = node.getChildren().get(1);
        } else {
            List<JmmNode> children = node.getChildren();
            if (children.size() != 4) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")),
                        "Method " + methodName + " isn't properly defined."));
                return false;
            }

            // return type
            JmmNode methodTypeNode = children.get(0);
            if (!methodTypeNode.getKind().equals("Type")) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(methodTypeNode.get("line")),
                        "Method " + methodName + " doesn't have a properly formatted return type."));
                return false;
            }
            returnType = new Type(methodTypeNode.get("dataType"), methodTypeNode.get("isArray").equals("yes"));

            // parameters
            JmmNode parametersNode = children.get(1);
            if (!parametersNode.getKind().equals("MethodParameters")) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(parametersNode.get("line")),
                        "Method " + methodName + " doesn't have a properly formatted parameters."));
                return false;
            }
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

            // body node
            bodyNode = children.get(2);

            // TODO verify return variable type to match method's type
            JmmNode returnNode = children.get(3);
        }

        // get local var declarations from body
        if (!bodyNode.getKind().equals("MethodBody")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(bodyNode.get("line")),
                    "Method " + methodName + " doesn't have a properly formatted body."));
            return false;
        }
        List<Symbol> localVars = new ArrayList<>();
        LocalVarsVisitor localVarsVisitor = new LocalVarsVisitor(methodParameters, localVars);
        // TODO in case of an error, we need to stop
        localVarsVisitor.visit(bodyNode, reports);

        this.symbolTable.addMethod(methodName, returnType, methodParameters, localVars);
        return true;
    }
}
