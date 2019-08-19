package pico.typecheck;

import org.checkerframework.checker.initialization.InitializationChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.SupportedOptions;

import java.util.Map.Entry;
import java.util.Set;

/**
 * Created by mier on 20/06/17.
 */
@SupportedOptions({"printFbcErrors"})
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
}
