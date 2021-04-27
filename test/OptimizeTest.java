
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

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.specs.util.SpecsIo;

public class OptimizeTest {

    /*
     *  helper
     */
    private void optimizeTest(String jmmCode) {
        var result = TestUtils.optimize(jmmCode);
        //TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testNachos() {
        String jmmCode = SpecsIo.read("test/nachotest.jmm");
        JmmParserResult result = TestUtils.parse(jmmCode);
        optimizeTest(jmmCode);
    }

    @Test
    public void testKazenga() {
        String jmmCode = SpecsIo.read("test/testedokazenga.jmm");
        JmmParserResult result = TestUtils.parse(jmmCode);
        // System.err.println(result.getRootNode().toJson());
        optimizeTest(jmmCode);
    }

    @Test
    public void testFindMaximum() {
        optimizeTest(SpecsIo.getResource("fixtures/public/FindMaximum.jmm"));
    }

    @Test
    public void testHelloWorld() {
        optimizeTest(SpecsIo.getResource("fixtures/public/HelloWorld.jmm"));
    }

    @Test
    public void testLazysort() {
        optimizeTest(SpecsIo.getResource("fixtures/public/Lazysort.jmm"));
    }

    @Test
    public void testLife() {
        optimizeTest(SpecsIo.getResource("fixtures/public/Life.jmm"));
    }

    @Test
    public void testMonteCarloPi() {
        optimizeTest(SpecsIo.getResource("fixtures/public/MonteCarloPi.jmm"));
    }

    @Test
    public void testQuickSort() {
        optimizeTest(SpecsIo.getResource("fixtures/public/QuickSort.jmm"));
    }

    @Test
    public void testSimple() {
        optimizeTest(SpecsIo.getResource("fixtures/public/Simple.jmm"));
    }

    @Test
    public void testTicTacToe() {
        optimizeTest(SpecsIo.getResource("fixtures/public/TicTacToe.jmm"));
    }

    @Test
    public void testWhileAndIF() {
        optimizeTest(SpecsIo.getResource("fixtures/public/WhileAndIF.jmm"));
    }
}
