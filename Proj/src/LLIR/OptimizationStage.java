package LLIR;

import Analysis.SymbolTable.MySymbolTable;
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright 2021 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

public class OptimizationStage implements JmmOptimization {
    private final boolean debug;

    public OptimizationStage(boolean debug) {
        this.debug = debug;
    }

    public OptimizationStage() {
        this.debug = false;
    }

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult, boolean optimize) {
        JmmNode node = semanticsResult.getRootNode();

        // Convert the AST to a String containing the equivalent OLLIR code
        OllirEmitter emitter = new OllirEmitter((MySymbolTable) semanticsResult.getSymbolTable(), optimize);
        String ollirCode = emitter.visit(node);

        // More reports from this stage
        List<Report> reports = new ArrayList<>();

        if (this.debug) System.err.println(ollirCode);

        return new OllirResult(semanticsResult, ollirCode, reports);
    }

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {
        return this.toOllir(semanticsResult, false);
    }

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        // THIS IS JUST FOR CHECKPOINT 3
        return semanticsResult;
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        // THIS IS JUST FOR CHECKPOINT 3
        return ollirResult;
    }
}
