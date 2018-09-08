package pico.inference;

import checkers.inference.BaseInferrableChecker;
import checkers.inference.InferenceAnnotatedTypeFactory;
import checkers.inference.InferenceChecker;
import checkers.inference.InferenceVisitor;
import checkers.inference.InferrableChecker;
import checkers.inference.SlotManager;
import checkers.inference.model.ConstraintManager;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.framework.source.SupportedOptions;
import pico.typecheck.PICOAnnotationMirrorHolder;

/**
 * Main entry class
 */
@SupportedOptions({"upcast", "anycast", "comparablecast", "optimalSolution"})
public class PICOInferenceChecker extends BaseInferrableChecker {

    @Override
    public void initChecker() {
        super.initChecker();
        PICOAnnotationMirrorHolder.init(this);
    }

    @Override
    public BaseAnnotatedTypeFactory createRealTypeFactory() {
        return new PICOInferenceRealTypeFactory(this, true);
    }

    @Override
    public InferenceAnnotatedTypeFactory createInferenceATF(InferenceChecker inferenceChecker, InferrableChecker realChecker, BaseAnnotatedTypeFactory realTypeFactory, SlotManager slotManager, ConstraintManager constraintManager) {
        return new PICOInferenceAnnotatedTypeFactory(inferenceChecker, realChecker.withCombineConstraints(), realTypeFactory, realChecker, slotManager, constraintManager);
    }

    @Override
    public InferenceVisitor<?, ?> createVisitor(InferenceChecker ichecker, BaseAnnotatedTypeFactory factory, boolean infer) {
        return new PICOInferenceVisitor(this, ichecker, factory, infer);
    }

    @Override
    public boolean withCombineConstraints() {
        return true;
    }

    @Override
    public boolean isInsertMainModOfLocalVar() {
        return true;
    }
}
