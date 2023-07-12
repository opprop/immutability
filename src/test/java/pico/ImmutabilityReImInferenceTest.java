package pico;

import checkers.inference.test.CFInferenceTest;
import org.checkerframework.framework.test.TestUtilities;
import org.checkerframework.javacutil.Pair;
import org.junit.Ignore;
import org.junit.runners.Parameterized.Parameters;
import pico.inference.PICOInferenceChecker;
import pico.inference.solver.PICOSolverEngine;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("initialization")
public class ImmutabilityReImInferenceTest extends CFInferenceTest {

    public ImmutabilityReImInferenceTest(File testFile) {
        super(testFile, PICOInferenceChecker.class, "",
              "-Anomsgtext",
                "-Astubs=src/main/java/pico/inference/jdk.astub",
                "-d", "testdata/reiminfer");
    }

    @Override
    public Pair<String, List<String>> getSolverNameAndOptions() {
        return Pair.of(PICOSolverEngine.class.getCanonicalName(),
                new ArrayList<>(Arrays.asList("useGraph=false", "collectStatistic=true")));
    }

    @Override
    public boolean useHacks() {
        return true;
    }

    @Override
    public boolean makeDefaultsExplicit() {
        return true;
    }

    @Parameters
    public static List<File> getTestFiles(){
        //InferenceTestUtilities.findAllSystemTests();
        return new ArrayList<>(TestUtilities.findRelativeNestedJavaFiles("testinput", "reiminfer"));
    }
}
