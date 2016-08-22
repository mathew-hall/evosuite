/**
 * Copyright (C) 2010-2016 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 *
 * This file is part of EvoSuite.
 *
 * EvoSuite is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3.0 of the License, or
 * (at your option) any later version.
 *
 * EvoSuite is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */
package org.evosuite.testsuite;

import org.evosuite.Properties;
import org.evosuite.Properties.Criterion;
import org.evosuite.coverage.FitnessFunctions;
import org.evosuite.coverage.branch.BranchCoverageSuiteFitness;
import org.evosuite.rmi.ClientServices;
import org.evosuite.statistics.RuntimeVariable;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.utils.LoggingUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RegressionTestSuiteSerialization {

  public RegressionTestSuiteSerialization() {
    // empty
  }

  public static void performRegressionAnalysis(TestSuiteChromosome testSuite) {

    List<TestChromosome> regressionTestChromosome = new ArrayList<TestChromosome>();

    File[] files = new File(Properties.SEED_DIR).listFiles();
    for (File file : files) {
      if (file.isFile() && file.getName().endsWith(".ser.back")) {
        regressionTestChromosome.addAll(TestSuiteSerialization.loadTests(file.getAbsolutePath()));
        // we already got it, so it seems safe to remove it as it will be overwritten anyway
        file.delete();
      }
    }

    int num_regression_tests = regressionTestChromosome.size();
    ClientServices.getInstance().getClientNode()
        .trackOutputVariable(RuntimeVariable.NumRegressionTestCases, num_regression_tests);

    LoggingUtils.getEvoLogger()
        .info("[RegressionTestSuiteSerialization] Number of regression test cases: "
            + num_regression_tests);
    LoggingUtils.getEvoLogger().info(
        "[RegressionTestSuiteSerialization] Branch fitness value of the generated test suite: "
            + testSuite.getFitnessInstanceOf(BranchCoverageSuiteFitness.class));

    // if we do not have a previous test suite or any previous test case
    // just serialize the generated test suite
    if (num_regression_tests == 0) {
      ClientServices.getInstance().getClientNode()
          .trackOutputVariable(RuntimeVariable.SizeAugmentedTestSuite, testSuite.getTests().size());

      serializedSuite(testSuite);
      return;
    }

    // Store this value; if this option is true then the JUnit check
    // would not succeed, as the JUnit classloader wouldn't find the class
    //boolean junitSeparateClassLoader = Properties.USE_SEPARATE_CLASSLOADER;
    //Properties.USE_SEPARATE_CLASSLOADER = false;

    TestSuiteChromosome regressionTestSuite = new TestSuiteChromosome();
    regressionTestSuite.addTests(regressionTestChromosome);

    // FIXME: add support to more than just BRANCH
    TestSuiteFitnessFunction branchFitness = FitnessFunctions.getFitnessFunction(Criterion.BRANCH);
    double branchFitnessValueRegressionSuite = branchFitness.getFitness(regressionTestSuite);
    LoggingUtils.getEvoLogger()
        .info("[RegressionTestSuiteSerialization] Fitness value of the regression test suite "
            + branchFitnessValueRegressionSuite);

    if (Properties.isRegression()) {
      // if 'Regression' mode, just join both test suites without checking coverage, etc.
      for (TestCase tc : regressionTestSuite.getTests()) {
        testSuite.addTest(tc);
      }
    } else {
      for (TestCase tc : testSuite.getTests()) {

        TestSuiteChromosome regressionTestSuiteClone = regressionTestSuite.clone();
        regressionTestSuiteClone.addTest(tc);

        double currentBranchFitnessValue = branchFitness.getFitness(regressionTestSuiteClone);
        if (branchFitnessValueRegressionSuite > currentBranchFitnessValue) {
          LoggingUtils.getEvoLogger().info(
              "[RegressionTestSuiteSerialization] [GOOD] we found a test case that improves branch fitness value of the regression test suite! ("
                  + branchFitnessValueRegressionSuite + " > " + currentBranchFitnessValue + ") :)");

          // augmenting the regression test suite with a new generated test case
          regressionTestSuite.addTest(tc);
          // and update fitness value
          branchFitnessValueRegressionSuite = currentBranchFitnessValue;
        } else {
          LoggingUtils.getEvoLogger().info("[RegressionTestSuiteSerialization] [BAD] this test case does not improve coverage! ("
              + branchFitnessValueRegressionSuite + " < " + currentBranchFitnessValue + ") :/");
        }
      }

      double finalBranchFitnessValueRegressionSuite = branchFitness.getFitness(regressionTestSuite);
      LoggingUtils.getEvoLogger().info("[RegressionTestSuiteSerialization] Are we ok? " + branchFitnessValueRegressionSuite
          + " == " + finalBranchFitnessValueRegressionSuite);
      if (branchFitnessValueRegressionSuite != finalBranchFitnessValueRegressionSuite) {
        LoggingUtils.getEvoLogger().info("[RegressionTestSuiteSerialization] Bloody hell fitness values are not equal!");
      }
      assert branchFitnessValueRegressionSuite == finalBranchFitnessValueRegressionSuite;

      // replace the generated test suite by an augmented one
      testSuite.clearTests();
      for (TestCase tc : regressionTestSuite.getTests()) {
        testSuite.addTest(tc);
      }
    }

    // write some statistics, e.g., number of regression test cases that
    // compile on the current version of the SUT
    LoggingUtils.getEvoLogger().info("[RegressionTestSuiteSerialization] Augmented suite has now "
        + testSuite.getTests().size() + " test cases");
    ClientServices.getInstance().getClientNode()
        .trackOutputVariable(RuntimeVariable.SizeAugmentedTestSuite, testSuite.getTests().size());

    // serialize augmented test suite
    serializedSuite(testSuite);
  }

  private static void serializedSuite(TestSuiteChromosome testSuite) {
    for (int i = 0; i < testSuite.getTestChromosomes().size(); i++) {
      TestChromosome tc = testSuite.getTestChromosomes().get(i);
      String filePath = Properties.SEED_DIR + File.separator
          + Properties.TARGET_CLASS.substring(Properties.TARGET_CLASS.lastIndexOf(".") + 1)
          + "_ESTest_" + i + ".ser";
      TestSuiteSerialization.saveTest(tc, new File(filePath));
    }
  }
}
