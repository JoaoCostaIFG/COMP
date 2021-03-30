import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.List;

public class MySymbolTable implements SymbolTable {
    private final List<String> imports;
    private String className, superName;
    private List<Symbol> classFields;

    public MySymbolTable() {
        this.imports = new ArrayList<>();
        this.className = null;
        this.superName = null;
        this.classFields = new ArrayList<>();
    }

    public void setImports(List<String> newImports) {
        if (newImports == null) return;
        this.imports.addAll(newImports);
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

    /**
     *
     * @return a list of Symbols that represent the fields of the class
     */
    @Override
    public List<Symbol> getFields() {
        return this.classFields;
    }

    @Override
    public List<String> getMethods() {
        return null;
    }

    @Override
    public Type getReturnType(String methodName) {
        return null;
    }

    @Override
    public List<Symbol> getParameters(String methodName) {
        return null;
    }

    @Override
    public List<Symbol> getLocalVariables(String methodName) {
        return null;
    }
}
