package pico.inference;

import org.checkerframework.checker.initialization.InitializationChecker;
import org.checkerframework.common.basetype.BaseTypeChecker;
import pico.typecheck.PICOAnnotationMirrorHolder;

public class PICOChecker extends InitializationChecker {

    public PICOChecker() {}

    @Override
    public Class<? extends BaseTypeChecker> getTargetCheckerClass() {
        return PICOInferenceChecker.class;
    }

    @Override
    public void initChecker() {
        super.initChecker();
        PICOAnnotationMirrorHolder.init(this);
    }

    @Override
    public boolean checkPrimitives() {
        return true;
    }

    @Override
    protected boolean shouldAddShutdownHook() {
        return super.shouldAddShutdownHook();
    }

    @Override
    protected void shutdownHook() {
        super.shutdownHook();
    }
}