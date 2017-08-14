package pico.inference;

import checkers.inference.InferenceChecker;
import checkers.inference.InferenceVisitor;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;

/**
 * Created by mier on 13/08/17.
 */
public class PICOInferenceVisitor extends InferenceVisitor {
    public PICOInferenceVisitor(InferenceChecker checker, InferenceChecker ichecker, BaseAnnotatedTypeFactory factory, boolean infer) {
        super(checker, ichecker, factory, infer);
    }
}
