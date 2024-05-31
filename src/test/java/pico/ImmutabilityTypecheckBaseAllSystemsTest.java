package pico;

import org.checkerframework.framework.test.CheckerFrameworkPerFileTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.Ignore;
import org.junit.runners.Parameterized.Parameters;
import pico.typecheck.PICOChecker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/* Focus on crashes and ignore errors for now. As errors are usually due to
 unannotated test files but crashes are due to bugs in CF */
@Ignore
public class ImmutabilityTypecheckBaseAllSystemsTest extends CheckerFrameworkPerFileTest {
    public ImmutabilityTypecheckBaseAllSystemsTest(File testFile) {
        super(testFile, PICOChecker.class, "", "-Anomsgtext", "-AsuppressWarnings=pico",
                "-Anocheckjdk", "-d", "testTmp/typecheck");
    }

    @Parameters
    public static List<File> getTestFiles(){
        return new ArrayList<>(TestUtilities.findRelativeNestedJavaFiles(
                "../checker-framework/checker/tests", "all-systems"));
    }
}
