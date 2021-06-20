package Analysis.SymbolTable;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.List;

public class Method {
    private final String uuid;
    private final List<Symbol> parameters;
    private final List<Symbol> localVars;
    private final Type returnType;
    private final JmmNode node;

    public Method(String uuid, Type returnType, List<Symbol> parameters, List<Symbol> localVars, JmmNode node) {
        this.uuid = uuid;
        this.parameters = parameters;
        this.localVars = localVars;
        this.returnType = returnType;
        this.node = node;
    }

    public String getId() {
        return uuid;
    }

    public String getName() {
        return this.uuid.split(":")[0];
    }

    public String getUUID() {
        return this.uuid.split(":")[1];
    }

    public boolean isMain() {
        return "main".equals(this.getName());
    }

    public List<Symbol> getParameters() {
        return this.parameters;
    }

    public List<Type> getParamTypes() {
        List<Type> ret = new ArrayList<>();
        for (int i = 0; i < this.parameters.size(); ++i) {
            ret.add(this.parameters.get(i).getType());
        }
        return ret;
    }

    public List<Symbol> getLocalVars() {
        return localVars;
    }

    public Symbol getVar(String name) {
        for (Symbol s : parameters)
            if (s.getName().equals(name))
                return s;
        for (Symbol s : localVars)
            if (s.getName().equals(name))
                return s;
        return null;
    }

    public Type getReturnType() {
        return returnType;
    }

    public JmmNode getNode() {
        return this.node;
    }

    public static String getNameFromString(String methodId) {
        return methodId.split(":")[0];
    }
}
