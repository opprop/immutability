package pico.inference;

import checkers.inference.InferenceAnnotatedTypeFactory;
import checkers.inference.InferenceChecker;
import checkers.inference.InferrableChecker;
import checkers.inference.SlotManager;
import checkers.inference.VariableAnnotator;
import checkers.inference.model.ConstraintManager;
import checkers.inference.model.VariableSlot;
import com.sun.source.tree.Tree;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;

/**
 * Propagates correct constraints on trees and types using TreeAnnotators and TypeAnnotators.
 * Add preference constraint on defaulting behaviour in typecheck mode, as the annotation can
 * be something different;
 * Replace VariableSlot that represents the annotation with ConstantSlot that is the implicit
 * type on that type. This ensures that that VariableSlot doesn't enter solver and solver doesn't
 * give solution to the VariableSlot, and there won't be annotations inserted to implicit locations.
 */
public class PICOInferenceAnnotatedTypeFactory extends InferenceAnnotatedTypeFactory {
    public PICOInferenceAnnotatedTypeFactory(InferenceChecker inferenceChecker, boolean withCombineConstraints, BaseAnnotatedTypeFactory realTypeFactory, InferrableChecker realChecker, SlotManager slotManager, ConstraintManager constraintManager) {
        super(inferenceChecker, withCombineConstraints, realTypeFactory, realChecker, slotManager, constraintManager);
    }

    @Override
    protected VariableAnnotator createVariableAnnotator(BaseAnnotatedTypeFactory realTypeFactory, InferrableChecker realChecker, SlotManager slotManager, ConstraintManager constraintManager) {
        return new VariableAnnotator(this, realTypeFactory, realChecker, slotManager, constraintManager) {
            @Override
            protected void handleClassDeclarationBound(AnnotatedDeclaredType classType) {
                return;
            }

            @Override
            protected void handleInstantiationConstraint(AnnotatedDeclaredType adt, VariableSlot instantiationSlot, Tree tree) {
                return;
            }
        };
    }
}
