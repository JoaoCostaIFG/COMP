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
    private int mainCount = 0;
    private final MySymbolTable symbolTable;

    public MethodVisitor(MySymbolTable symbolTable) {
        super();
        this.mainCount = 0;
        this.symbolTable = symbolTable;
        addVisit("MethodDeclaration", this::parseMethodDeclaration);
    }

    // IMP local vars can't have the same name has method parameters

    private Boolean parseMethodDeclaration(JmmNode node, List<Report> reports) {
        String methodName = node.get("methodName");

        if (methodName.equals("main"))  // is main
            return this.visitMainFunction(node, methodName, reports);
        else
            return this.visitClassMethod(node, methodName, reports);
    }

    private List<Symbol> visitLocalVrs(JmmNode bodyNode, List<Symbol> methodParameters, List<Report> reports) {
        List<Symbol> localVars = new ArrayList<>();
        LocalVarsVisitor localVarsVisitor = new LocalVarsVisitor(symbolTable, methodParameters, localVars);
        localVarsVisitor.visit(bodyNode, reports);
        return localVarsVisitor.isSuccess() ? localVars : null;
    }

    private List<Symbol> visitBody(JmmNode bodyNode, String methodName, List<Symbol> methodParameters, List<Report> reports) {
        // get local var declarations from body
        if (!bodyNode.getKind().equals("MethodBody")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(bodyNode.get("line")),
                    "Method " + methodName + " doesn't have a properly formatted body."));
            return null;
        }

        return this.visitLocalVrs(bodyNode, methodParameters, reports);
    }

    private boolean visitMainFunction(JmmNode node, String methodName, List<Report> reports) {
        // only allow a single main definition
        if (this.mainCount >= 1) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")),
                    "Redeclaration of main function."));
            return false;
        }
        ++this.mainCount;
        // return type
        Type returnType = new Type("void", false);
        // parameter
        JmmNode mainParam = node.getChildren().get(0);
        if (!mainParam.getKind().equals("MainParameter")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(mainParam.get("line")),
                    "Main parameter name isn't defined."));
            return false;
        }
        String paramName = mainParam.get("paramName");
        // // conflicts with class field
        // if (this.symbolTable.getField(paramName) != null) {
        //     reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(mainParam.get("line")),
        //             "Main parameter name conflicts with class field name: " + paramName + "."));
        //     return false;
        // }
        List<Symbol> methodParameters = new ArrayList<>();
        methodParameters.add(new Symbol(new Type("String", true), paramName));

        // body node
        JmmNode bodyNode = node.getChildren().get(1);
        List<Symbol> localVars = this.visitBody(bodyNode, methodName, methodParameters, reports);
        if (localVars == null) return false;

        this.symbolTable.addMethod(methodName, returnType, methodParameters, localVars, node);
        return true;
    }

    private boolean visitClassMethod(JmmNode node, String methodName, List<Report> reports) {
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
        Type returnType = new Type(methodTypeNode.get("dataType"), methodTypeNode.get("isArray").equals("yes"));

        // parameters
        JmmNode parametersNode = children.get(1);
        if (!parametersNode.getKind().equals("MethodParameters")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(parametersNode.get("line")),
                    "Method " + methodName + " doesn't have a properly formatted parameters."));
            return false;
        }
        List<Symbol> methodParameters = new ArrayList<>();
        for (JmmNode paramNode : parametersNode.getChildren()) {
            String paramName = paramNode.get("paramName");
            // // conflicts with class field
            // if (this.symbolTable.getField(paramName) != null) {
            //     reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(paramNode.get("line")),
            //             "Method parameter name conflicts with class field name: " + paramName + "."));
            //     return false;
            // }
            // parameter type
            JmmNode typeNode = paramNode.getChildren().get(0);
            Type paramType = new Type(typeNode.get("dataType"), typeNode.get("isArray").equals("yes"));
            methodParameters.add(new Symbol(paramType, paramName));
        }
        // check if is overload/overloaded correctly
        boolean conflicts = false;
        for (Method m : this.symbolTable.getOverloads(methodName)) {
            List<Symbol> mParams = m.getParameters();
            if (methodParameters.size() != mParams.size()) continue;

            boolean areTheSame = true;
            for (int i = 0; i < mParams.size(); i++) {
                Type t1 = mParams.get(i).getType();
                Type t2 = methodParameters.get(i).getType();
                if (!t1.getName().equals(t2.getName()) || t1.isArray() != t2.isArray()) {
                    areTheSame = false;
                    break;
                }
            }

            if (areTheSame) {
                conflicts = true;
                break;
            }
        }
        if (conflicts) {  // in case it conflicts with something
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, parseInt(node.get("line")),
                    "Method " + methodName + " is already defined."));
            return false;
        }

        // get local vars of the body node
        JmmNode bodyNode = children.get(2);
        List<Symbol> localVars = this.visitBody(bodyNode, methodName, methodParameters, reports);
        if (localVars == null) return false;

        // TODO verify return variable type to match method's type
        JmmNode returnNode = children.get(3);

        this.symbolTable.addMethod(methodName, returnType, methodParameters, localVars, node);
        return true;
    }
}
