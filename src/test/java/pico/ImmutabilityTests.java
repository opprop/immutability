package pico;

import org.checkerframework.framework.test.CheckerFrameworkPerFileTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;
import pico.typecheck.PICOChecker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImmutabilityTests extends CheckerFrameworkPerFileTest {
    public ImmutabilityTests(File testFile) {
        super(testFile, PICOChecker.class, "", "-Anomsgtext", "-d", "testTmp");
    }

    @Parameters
    public static List<File> getTestFiles(){
        List<File> testfiles = new ArrayList<>();
        testfiles.addAll(TestUtilities.findRelativeNestedJavaFiles("testinput", "pico"));
        return testfiles;
    }
}