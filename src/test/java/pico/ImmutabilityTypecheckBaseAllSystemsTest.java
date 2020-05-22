package pico;

import org.checkerframework.framework.test.CheckerFrameworkPerFileTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;
import pico.typecheck.PICOChecker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImmutabilityTypecheckBaseAllSystemsTest extends CheckerFrameworkPerFileTest {
    public ImmutabilityTypecheckBaseAllSystemsTest(File testFile) {
        super(testFile, PICOChecker.class, "", "-Anomsgtext",
                "-Anocheckjdk", "-d", "testTmp/typecheck");
    }

    @Parameters
    public static List<File> getTestFiles(){
        List<File> testfiles = new ArrayList<>(TestUtilities.findRelativeNestedJavaFiles(
                "../checker-framework/checker/tests", "all-systems"));
        return testfiles;
    }
}
