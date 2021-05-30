
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
import pt.up.fe.specs.util.SpecsStrings;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class BackendTest {
    private String test(String code, List<String> args, String input) {
        // IMP the code of TestUtils.backend(String code) was copied here so we can enable optimizations.
        var ollirResult = TestUtils.optimize(code, true);
        TestUtils.noErrors(ollirResult.getReports());
        var result = TestUtils.backend(ollirResult);

        TestUtils.noErrors(result.getReports());
        if (input == null) return result.run(args);
        return result.run(args, input);
    }

    private String test(String code, List<String> args) {
        return test(code, args, null);
    }

    private void test(String code, String expectedResult, List<String> args) {
        assertEquals(SpecsStrings.normalizeFileContents(expectedResult),
                SpecsStrings.normalizeFileContents(test(code, args)));
    }


    private void test(String code, String expectedResult) {
        test(code, expectedResult, Collections.emptyList());
    }

    @Test
    public void KazengaTest() {
        test(SpecsIo.read("test/testedokazenga.jmm"), "45\n");
    }

    @Test
    public void testFindMaximum() {
        test(SpecsIo.getResource("fixtures/public/FindMaximum.jmm"),
                "Result: 28\n");
    }

    @Test
    public void testHelloWorld() {
        test(SpecsIo.getResource("fixtures/public/HelloWorld.jmm"),
                "Hello, World!\n");
    }

    @Test
    public void testLazysort() {
        // the beLazy method makes no sense (?)
        // this code is not a sort and actually just shuffles the array most of the time
        // so we just test if the output is a list of 10 integers [0, 11].
        String testResult = test(SpecsIo.getResource("fixtures/public/Lazysort.jmm"),
                Collections.emptyList());
        System.out.println(testResult);
        String[] resultSplit = testResult.split("\n");
        for (String s : resultSplit) {
            int i = Integer.parseInt(s);
            if (i < 0 || i > 11)
                fail("Number " + s + "out of range");
        }
    }

    @Test
    public void testQuickSort() {
        test(SpecsIo.getResource("fixtures/public/QuickSort.jmm"),
                "1\n2\n3\n4\n5\n6\n7\n8\n9\n10\n");
    }

    @Test
    public void testSimple() {
        test(SpecsIo.getResource("fixtures/public/Simple.jmm"), "30\n");
    }

    @Test
    public void testWhileAndIF() {
        test(SpecsIo.getResource("fixtures/public/WhileAndIF.jmm"),
                "10\n10\n10\n10\n10\n10\n10\n10\n10\n10\n");
    }

    /* INTERACTIVE PROGRAMS (NEED USER INPUT) */
    @Test
    public void testLife() {
        // this game is interactive and has no end: enter any non-empty input to step the game
        // this game is infinite so we send it some bad input so it crashes after 9 steps
        test(SpecsIo.getResource("fixtures/public/Life.jmm"), Collections.emptyList(), "1\n2\n3\n4\n5\n6\n7\n8\n9\n\n");
    }

    @Test
    public void testMonteCarloPi() {
        // this test expects user input => no output can be expected without mocking user input
        test(SpecsIo.getResource("fixtures/public/MonteCarloPi.jmm"), Collections.emptyList(),
                "1000000\n");
    }

    @Test
    public void testTicTacToe() {
        // this test is an interactive game => no output can be expected without mocking user input
        test(SpecsIo.getResource("fixtures/public/TicTacToe.jmm"), Collections.emptyList(),
                "0\n0\n0\n1\n1\n0\n1\n1\n2\n0\n");
    }

    /* OUR EXAMPLE PROGRAMS */
    @Test
    public void binarySearchTest() {
        test(SpecsIo.getResource("MyExamplePrograms/BinarySearch.jmm"), "5\n");
    }

    @Test
    public void constantsOptimizationTest() {
        test(SpecsIo.getResource("MyExamplePrograms/ConstantsOptimizations.jmm"), "-372\n");
    }

    @Test
    public void fibonacciTest() {
        test(SpecsIo.getResource("MyExamplePrograms/Fibonacci.jmm"), Collections.emptyList(),
                "11\n");
    }

    @Test
    public void inferenceTest() {
        test(SpecsIo.getResource("MyExamplePrograms/Inference.jmm"), Collections.emptyList());
    }

    @Test
    public void skaneTest() {
        // this test is an interactive game => no output can be expected without mocking user input
        test(SpecsIo.getResource("MyExamplePrograms/Skane.jmm"), Collections.emptyList(), "7\n7\n8\n\n");
    }

    @Test
    public void staticClassFieldsTest() {
        test(SpecsIo.getResource("MyExamplePrograms/StaticClassFields.jmm"), Collections.emptyList());
    }

    @Test
    public void varNotInitTest() {
        test(SpecsIo.getResource("MyExamplePrograms/VarNotInit.jmm"), Collections.emptyList());
    }
}
