import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

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
        return parameters;
    }

    public List<Symbol> getLocalVars() {
        return localVars;
    }

    public Type getReturnType() {
        return returnType;
    }

    public JmmNode getNode() {
        return this.node;
    }
}
