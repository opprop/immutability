package pico.typecheck;

import org.checkerframework.checker.initialization.InitializationChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;

/**
 * Created by mier on 20/06/17.
 */
public class PICOChecker extends InitializationChecker {

    public PICOChecker() {
        super(true);
    }

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new PICOVisitor(this);
    }
}
