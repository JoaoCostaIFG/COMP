
/**
 * Copyright 2021 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

public class BackendTest {
    private void test(String code, String expectedResult) {
        var result = TestUtils.backend(code);
        TestUtils.noErrors(result.getReports());

        var output = result.run();
        assertEquals(expectedResult, output.trim());
    }

    @Test
    public void KazengaTest() {
        test(SpecsIo.read("test/testedokazenga.jmm"), "6");
    }

    @Test
    public void testFindMaximum() {
        test(SpecsIo.getResource("fixtures/public/FindMaximum.jmm"), "Result: 28");
    }

    @Test
    public void testHelloWorld() {
        test(SpecsIo.getResource("fixtures/public/HelloWorld.jmm"), "Hello, World!");
    }

    @Test
    public void testLazysort() {
        // TODO no class Quicksort found
        test(SpecsIo.getResource("fixtures/public/Lazysort.jmm"), "a");
    }

    @Test
    public void testLife() {
        // TODO unable to pop from empty stack
        test(SpecsIo.getResource("fixtures/public/Life.jmm"), "a");
    }

    @Test
    public void testMonteCarloPi() {
        // TODO unable to pop from empty stack
        test(SpecsIo.getResource("fixtures/public/MonteCarloPi.jmm"), "a");
    }

    @Test
    public void testQuickSort() {
        test(SpecsIo.getResource("fixtures/public/QuickSort.jmm"), "1\n2\n3\n4\n5\n6\n7\n8\n9\n10");
    }

    @Test
    public void testSimple() {
        test(SpecsIo.getResource("fixtures/public/Simple.jmm"), "30");
    }

    @Test
    public void testTicTacToe() {
        // TODO unable to pop from empty stack (method: winner)
        test(SpecsIo.getResource("fixtures/public/TicTacToe.jmm"), "a");
    }

    @Test
    public void testWhileAndIF() {
        test(SpecsIo.getResource("fixtures/public/WhileAndIF.jmm"), "0\n0\n10\n10\n10\n10\n10\n10\n10\n10");
    }
}
