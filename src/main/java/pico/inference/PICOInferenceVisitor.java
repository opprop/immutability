package pico.inference;

import checkers.inference.InferenceChecker;
import checkers.inference.InferenceValidator;
import checkers.inference.InferenceVisitor;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;

/**
 * Generate constraints based on the PICO constraint-based type rules in infer mode. Has typecheck
 * and infer mode. In typecheck mode, has the exact same behaviour as PICOVisitor.
 */
public class PICOInferenceVisitor extends InferenceVisitor {
    public PICOInferenceVisitor(InferenceChecker checker, InferenceChecker ichecker, BaseAnnotatedTypeFactory factory, boolean infer) {
        super(checker, ichecker, factory, infer);
    }

    @Override
    protected InferenceValidator createTypeValidator() {
        return new PICOInferenceValidator(checker, this, atypeFactory);
    }
}
