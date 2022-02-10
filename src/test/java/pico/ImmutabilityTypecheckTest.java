package pico;

import org.checkerframework.framework.test.CheckerFrameworkPerFileTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;
import pico.typecheck.PICOChecker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImmutabilityTypecheckTest extends CheckerFrameworkPerFileTest {
    public ImmutabilityTypecheckTest(File testFile) {
        super(testFile, PICOChecker.class, "", "-Anomsgtext",
                "-Anocheckjdk", "-d", "testTmp/typecheck");
    }

    @Parameters
    public static List<File> getTestFiles(){
        List<File> testfiles = new ArrayList<>();
        testfiles.addAll(TestUtilities.findRelativeNestedJavaFiles("testinput", "typecheck"));
        return testfiles;
    }
}
