package pico.inference;

import checkers.inference.*;
import checkers.inference.model.ConstraintManager;
import org.checkerframework.checker.initialization.InitializationFieldAccessSubchecker;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.SupportedOptions;
import pico.typecheck.PICOAnnotationMirrorHolder;

import java.util.Set;

/**
 * Main entry class
 */
@SupportedOptions({"upcast", "anycast", "comparablecast", "optimalSolution", "useOptimisticUncheckedDefaults"})
public class PICOInferenceChecker extends BaseInferrableChecker {

    @Override
    public void initChecker() {
        super.initChecker();
        PICOAnnotationMirrorHolder.init(this);
    }

    @Override
    public BaseInferenceRealTypeFactory createRealTypeFactory(boolean infer) {
        return new PICOInferenceRealTypeFactory(this, infer);
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
    public PICOInferenceRealTypeFactory getTypeFactory() {
        return (PICOInferenceRealTypeFactory) super.getTypeFactory();
    }

    @Override
    protected Set<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
        Set<Class<? extends BaseTypeChecker>> checkers = super.getImmediateSubcheckerClasses();
        checkers.add(InitializationFieldAccessSubchecker.class);
        return checkers;
    }

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        // This is a hacky way to get source visitor
        return new PICOInferenceVisitor(this, this, this.createRealTypeFactory(false), false);
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
