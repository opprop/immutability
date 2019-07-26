package pico;

import checkers.inference.test.CFInferenceTest;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.checkerframework.framework.test.TestUtilities;
import org.checkerframework.javacutil.Pair;
import org.junit.runners.Parameterized.Parameters;
import pico.inference.PICOInferenceChecker;
import pico.inference.solver.PICOSolverEngine;

public class ImmutabilityInferenceTest extends CFInferenceTest {

    public ImmutabilityInferenceTest(File testFile) {
        super(
                testFile,
                PICOInferenceChecker.class,
                "",
                "-Anomsgtext",
                "-Astubs=src/main/java/pico/typecheck/jdk.astub",
                "-d",
                "testdata/inference/inferrable");
    }

    @Override
    public Pair<String, List<String>> getSolverNameAndOptions() {
        return Pair.<String, List<String>>of(
                PICOSolverEngine.class.getCanonicalName(),
                new ArrayList<String>(Arrays.asList("useGraph=false", "collectStatistic=true")));
    }

    @Override
    public boolean useHacks() {
        return true;
    }

    @Parameters
    public static List<File> getTestFiles() {
        List<File> testfiles = new ArrayList<>(); // InferenceTestUtilities.findAllSystemTests();
        testfiles.addAll(
                TestUtilities.findRelativeNestedJavaFiles("testinput", "inference/inferrable"));
        return testfiles;
    }
}
