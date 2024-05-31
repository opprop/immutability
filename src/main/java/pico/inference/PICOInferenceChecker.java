package pico.inference;

import checkers.inference.*;
import checkers.inference.model.ConstraintManager;
import org.checkerframework.checker.initialization.InitializationFieldAccessSubchecker;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.reflection.MethodValChecker;
import org.checkerframework.framework.source.SupportedOptions;
import pico.typecheck.PICOAnnotationMirrorHolder;

import java.util.Set;

/**
 * Main entry class
 * useForInference is a key for inference task only as the current inference does not support initialization inference
 */
@SupportedOptions({"upcast", "anycast", "comparablecast", "optimalSolution", "useOptimisticUncheckedDefaults", "useForInference"})
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

    private Set<Class<? extends BaseTypeChecker>> cachedCheckers = null;

    @Override
    protected Set<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
        if (cachedCheckers == null) {
            cachedCheckers = super.getImmediateSubcheckerClasses();
            if (!hasOption("useForInference")) {
                cachedCheckers.add(InitializationFieldAccessSubchecker.class);
            }
        }
        return cachedCheckers;
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
