package Backend;

import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.OllirErrorException;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.Collections;
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

public class BackendStage implements JasminBackend {
    private final int registersLimit;

    public BackendStage(int registersLimit) {
        this.registersLimit = registersLimit;
    }

    public BackendStage() {
        this(0);
    }

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        // More reports from this stage
        List<Report> reports = new ArrayList<>();
        ClassUnit ollirClass = ollirResult.getOllirClass();

        try {
            ollirClass.checkMethodLabels(); // check the use of labels in the OLLIR loaded
            ollirClass.buildCFGs(); // build the CFG of each method
            ollirClass.buildVarTables(); // build the table of variables for each method

            // Convert the OLLIR to a String containing the equivalent Jasmin code
            JasminEmitter jasminEmitter = new JasminEmitter(ollirClass, reports, this.registersLimit);
            String jasminCode = jasminEmitter.parse();

            return new JasminResult(ollirResult, jasminCode, reports);
        } catch (OllirErrorException e) {
            return new JasminResult(ollirClass.getClassName(), null,
                    Collections.singletonList(Report.newError(Stage.GENERATION, -1, -1,
                            "Exception during Jasmin generation", e)));
        }
    }
}