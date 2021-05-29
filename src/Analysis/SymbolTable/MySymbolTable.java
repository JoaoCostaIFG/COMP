package Analysis.SymbolTable;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.*;

public class MySymbolTable implements SymbolTable {
    private final List<String> imports;
    private String className, superName;
    private final List<Symbol> classFields;
    private final Map<String, Method> methods;

    public MySymbolTable() {
        this.imports = new ArrayList<>();
        this.className = null;
        this.superName = null;
        this.classFields = new ArrayList<>();
        this.methods = new HashMap<>();
    }

    public void setImports(List<String> newImports) {
        if (newImports == null) return;
        this.imports.addAll(newImports);
    }

    public boolean hasImport(String methodName) {
        for (String imp : imports) {
            String[] parsed = imp.split("\\.");
            String lastImport = parsed[parsed.length - 1];
            if (lastImport.equals(methodName))
                return true;
        }
        return false;
    }

    @Override
    public List<String> getImports() {
        return imports;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public String getClassName() {
        return this.className;
    }

    public void setSuper(String superName) {
        this.superName = superName;
    }

    @Override
    public String getSuper() {
        return this.superName;
    }

    public void setFields(List<Symbol> newFields) {
        if (newFields == null) return;
        this.classFields.addAll(newFields);
    }

    public Symbol getField(String name) {
        for (Symbol s : this.classFields) {
            if (s.getName().equals(name))
                return s;
        }
        return null;
    }

    /**
     * @return a list of Symbols that represent the fields of the class
     */
    @Override
    public List<Symbol> getFields() {
        return this.classFields;
    }

    public String addMethod(String methodName, Type returnType, List<Symbol> parameters, List<Symbol> localVars, JmmNode node) {
        // 2 methods can have the same => append UUID to method name
        // ":" can't be part of a method's name
        String methodUUID = methodName + ":" + UUID.randomUUID().toString();
        this.methods.put(methodUUID, new Method(methodUUID, returnType, parameters, localVars, node));
        return methodUUID;
    }

    public List<Method> getOverloads(String name) {
        List<Method> overloads = new ArrayList<>();
        for (Map.Entry<String, Method> e : this.methods.entrySet()) {
            Method m = e.getValue();
            if (m.getName().equals(name))
                overloads.add(m);
        }

        return overloads;
    }

    public Method getMethodByCall(String methodName, List<Type> paramsTypes) {
        for (Method method : this.methods.values()) {
            if (!method.getName().equals(methodName))
                continue;

            List<Symbol> params = method.getParameters();
            if (params.size() != paramsTypes.size()) continue;

            boolean isEqual = true;
            for (int i = 0; i < params.size(); ++i) {
                Type paramType = params.get(i).getType();
                Type paramToTest = paramsTypes.get(i);
                if (!paramType.getName().equals(paramToTest.getName()) ||
                        paramType.isArray() != paramToTest.isArray()) {
                    isEqual = false;
                    break;
                }
            }
            if (isEqual)
                return method;
        }

        return null;
    }

    public Method getMethod(String methodName) {
        return this.methods.get(methodName);
    }

    @Override
    public List<String> getMethods() {
        return new ArrayList<>(this.methods.keySet());
    }

    @Override
    public Type getReturnType(String methodName) {
        if (!this.methods.containsKey(methodName))
            return null;
        return this.methods.get(methodName).getReturnType();
    }

    @Override
    public List<Symbol> getParameters(String methodName) {
        if (!this.methods.containsKey(methodName))
            return null;
        return this.methods.get(methodName).getParameters();
    }

    @Override
    public List<Symbol> getLocalVariables(String methodName) {
        if (!this.methods.containsKey(methodName))
            return null;
        return this.methods.get(methodName).getLocalVars();
    }
}
