package org.evosuite.testcase;

import com.examples.with.different.packagename.TrivialInt;
import com.examples.with.different.packagename.strings.TrivialString;
import org.evosuite.Properties;
import org.evosuite.SystemTestBase;
import org.evosuite.TestGenerationContext;
import org.evosuite.coverage.branch.BranchCoverageSuiteFitness;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.testcase.statements.MethodStatement;
import org.evosuite.testcase.statements.StringPrimitiveStatement;
import org.evosuite.testcase.statements.numeric.IntPrimitiveStatement;
import org.evosuite.testcase.variable.VariableReference;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.utils.generic.GenericClass;
import org.evosuite.utils.generic.GenericConstructor;
import org.evosuite.utils.generic.GenericMethod;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by mat on 30/08/2016.
 */
public class LanguageModelMutationSystemTest extends SystemTestBase {


    public TestCase getStringTest(String input) throws NoSuchMethodException, SecurityException, ConstructionFailedException, ClassNotFoundException{
        Class<?> sut = TestGenerationContext.getInstance().getClassLoaderForSUT().loadClass(Properties.TARGET_CLASS);
        GenericClass clazz = new GenericClass(sut);

        DefaultTestCase test = new DefaultTestCase();

        VariableReference arg = test.addStatement(new StringPrimitiveStatement(test, input));

        Method m = clazz.getRawClass().getMethod("testSomething", new Class<?>[] { String.class});
        GenericMethod method = new GenericMethod(m, sut);
        MethodStatement ms = new MethodStatement(test, method, null, Arrays.asList(new VariableReference[] {arg}));
        test.addStatement(ms);

        return test;
    }

    @Test
    public void testReadabilityMutation() throws NoSuchMethodException, SecurityException, ClassNotFoundException, ConstructionFailedException {
        Properties.TARGET_CLASS = TrivialString.class.getCanonicalName();
        TestChromosome test1 = new TestChromosome();
        test1.setTestCase(getStringTest("w4ff135"));

        TestSuiteChromosome suite = new TestSuiteChromosome();

        suite.addTest(test1);

        Properties.P_TEST_CHANGE  = 1.0;
        Properties.P_TEST_DELETE  = 0.0;
        Properties.P_TEST_INSERT  = 0.0;
        Properties.PRIMITIVE_POOL = 0.0;
        Properties.LANGUAGE_MODEL = 1.0;

        for(int i = 0; i < 10000; i++) {
            test1.mutate();
        }


        assertNotEquals("w4ff135", ((StringPrimitiveStatement)test1.getTestCase().getStatement(0)).getValue());
    }
}
