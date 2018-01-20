package pico.typecheck;

import org.checkerframework.checker.initialization.InitializationChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.SupportedLintOptions;

import java.util.Map.Entry;
import java.util.Set;

/**
 * Created by mier on 20/06/17.
 */
@SupportedLintOptions({"printFbcErrors"})
public class PICOChecker extends InitializationChecker {

    public PICOChecker() {
        super(true);
    }

    @Override
    public void initChecker() {
        super.initChecker();
        PICOAnnotationMirrorHolder.init(this);
    }

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new PICOVisitor(this);
    }

    @Override
    protected boolean shouldAddShutdownHook() {
        return getLintOption("printFbcErrors", false) || super.shouldAddShutdownHook();
    }

    @Override
    protected void shutdownHook() {
        super.shutdownHook();
        if (getLintOption("printFbcErrors", false)) {
            printFbcViolatedMethods();
        }
    }

    private void printFbcViolatedMethods() {
        Set<Entry<String, Integer>> entries = ((PICOVisitor) visitor).fbcViolatedMethods.entrySet();
        if (entries.isEmpty()) {
            System.out.println("\n=============== Congrats! No Fbc Violations Found. ===============\n");
        } else {
            System.out.println("\n===================== Fbc Violations Found! ======================");
            System.out.format("%30s%30s\n", "Method", "Violated Times");
            for (Entry<String, Integer> e : entries) {
                System.out.format("%30s%30s\n", e.getKey(), e.getValue());
            }
            System.out.println("====================================================================\n");
        }
    }
}
