
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
import pt.up.fe.specs.util.SpecsIo;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class BackendTest {
    private void test(String code, String expectedResult, List<String> args) {
        var result = TestUtils.backend(code);
        TestUtils.noErrors(result.getReports());

        var output = result.run(args);
        assertEquals(expectedResult, output.trim());
    }

    private void test(String code, String expectedResult) {
        test(code, expectedResult, Collections.emptyList());
    }

    @Test
    public void KazengaTest() {
        test(SpecsIo.read("test/testedokazenga.jmm"), "6");
    }

    @Test
    public void testFindMaximum() {
        test(SpecsIo.getResource("fixtures/public/FindMaximum.jmm"),
                "Result: 28");
    }

    @Test
    public void testHelloWorld() {
        test(SpecsIo.getResource("fixtures/public/HelloWorld.jmm"),
                "Hello, World!");
    }

    @Test
    public void testLazysort() {
        // TODO the beLazy method makes no sense
        test(SpecsIo.getResource("fixtures/public/Lazysort.jmm"),
                "1\n2\n3\n4\n5\n6\n7\n8\n9\n10");
    }

    @Test
    public void testLife() {
        // TODO invalid local variable number: field method
        test(SpecsIo.getResource("fixtures/public/Life.jmm"), "a");
    }

    @Test
    public void testMonteCarloPi() {
        // this test expects user input => no output can be expected without mocking user input
        test(SpecsIo.getResource("fixtures/public/MonteCarloPi.jmm"), "a");
    }

    @Test
    public void testQuickSort() {
        test(SpecsIo.getResource("fixtures/public/QuickSort.jmm"),
                "1\n2\n3\n4\n5\n6\n7\n8\n9\n10");
    }

    @Test
    public void testSimple() {
        test(SpecsIo.getResource("fixtures/public/Simple.jmm"), "30");
    }

    @Test
    public void testTicTacToe() {
        // String inputStr = "0\n0\n1\n1\n";
        // InputStream inputSS = new java.io.ByteArrayInputStream(inputStr.getBytes());
        // System.setIn(inputSS);

        // this test is an interactive game => no output can be expected without mocking user input
        test(SpecsIo.getResource("fixtures/public/TicTacToe.jmm"), "a");
    }

    @Test
    public void testWhileAndIF() {
        test(SpecsIo.getResource("fixtures/public/WhileAndIF.jmm"),
                "10\n10\n10\n10\n10\n10\n10\n10\n10\n10");
    }
}
