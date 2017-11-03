package pico.inference;

import checkers.inference.InferenceValidator;
import checkers.inference.InferenceVisitor;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;

/**
 * Generates constraints based on PICO constraint-based well-formedness rules in infer mode.
 * In typecheck mode, it behaves exactly like PICOValidator
 */
public class PICOInferenceValidator extends InferenceValidator{
    public PICOInferenceValidator(BaseTypeChecker checker, InferenceVisitor<?, ?> visitor, AnnotatedTypeFactory atypeFactory) {
        super(checker, visitor, atypeFactory);
    }
}
