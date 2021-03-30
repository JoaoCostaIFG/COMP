import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.List;

public class Method {
    private final String uuid;
    private final List<Symbol> parameters;
    private final List<Symbol> localVars;
    private final Type returnType;

    public Method(String uuid, Type returnType, List<Symbol> parameters, List<Symbol> localVars) {
        this.uuid = uuid;
        this.parameters = parameters;
        this.localVars = localVars;
        this.returnType = returnType;
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return this.uuid.split("-")[0];
    }

    public String getUUID() {
        return this.uuid.split("-")[1];
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
}
