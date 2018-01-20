package pico.inference;

import checkers.inference.BaseInferrableChecker;
import checkers.inference.InferenceAnnotatedTypeFactory;
import checkers.inference.InferenceChecker;
import checkers.inference.InferenceVisitor;
import checkers.inference.InferrableChecker;
import checkers.inference.SlotManager;
import checkers.inference.model.ConstraintManager;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.javacutil.AnnotationBuilder;
import pico.typecheck.PICOAnnotationMirrorHolder;
import qual.Bottom;
import qual.Immutable;
import qual.Mutable;
import qual.Readonly;
import qual.ReceiverDependantMutable;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;

/**
 * Main entry class
 */
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
