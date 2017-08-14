package pico.typecheck;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeFactory;

/**
 * Created by mier on 24/06/17.
 */
public class PICOValidator extends BaseTypeValidator {
    public PICOValidator(BaseTypeChecker checker, BaseTypeVisitor<?> visitor, AnnotatedTypeFactory atypeFactory) {
        super(checker, visitor, atypeFactory);
    }

}
