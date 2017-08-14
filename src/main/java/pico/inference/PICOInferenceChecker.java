package pico.inference;

import checkers.inference.BaseInferrableChecker;
import checkers.inference.InferenceAnnotatedTypeFactory;
import checkers.inference.InferenceChecker;
import checkers.inference.InferenceVisitor;
import checkers.inference.InferrableChecker;
import checkers.inference.SlotManager;
import checkers.inference.model.ConstraintManager;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import pico.typecheck.PICOAnnotatedTypeFactory;

/**
 * Created by mier on 13/08/17.
 */
public class PICOInferenceChecker extends BaseInferrableChecker{
    @Override
    public BaseAnnotatedTypeFactory createRealTypeFactory() {
        // Return the GUTIAnnotatedTypeFactory so that it can carry
        // GUTIVariableAnnotator
        return new PICOAnnotatedTypeFactory(this);
    }

    @Override
    public InferenceVisitor<?, ?> createVisitor(InferenceChecker checker,
                                                BaseAnnotatedTypeFactory factory, boolean infer) {
        return new PICOInferenceVisitor(this, checker, factory, infer);
    }

    @Override
    public boolean withCombineConstraints() {
        return true;
    }

    @Override
    public InferenceAnnotatedTypeFactory createInferenceATF(
            InferenceChecker inferenceChecker, InferrableChecker realChecker,
            BaseAnnotatedTypeFactory realTypeFactory, SlotManager slotManager,
            ConstraintManager constraintManager) {
        return new PICOInferenceAnnotatedTypeFactory(inferenceChecker,
                withCombineConstraints(), realTypeFactory, realChecker,
                slotManager, constraintManager);
    }

    @Override
    public boolean isInsertMainModOfLocalVar() {
        return true;
    }
}
