package pico;

import org.checkerframework.framework.test.CheckerFrameworkPerFileTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.Ignore;
import org.junit.runners.Parameterized.Parameters;
import pico.typecheck.PICOChecker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImmutabilityTypecheckGlacierTest extends CheckerFrameworkPerFileTest {
    public ImmutabilityTypecheckGlacierTest(File testFile) {
        super(testFile, PICOChecker.class, "", "-Anomsgtext",
                "-Anocheckjdk", "-d", "testTmp/glacier");
    }

    @Parameters
    public static List<File> getTestFiles(){
        return new ArrayList<>(TestUtilities.findRelativeNestedJavaFiles("testinput", "glacier"));
    }
}
