package pico.inference;

import checkers.inference.InferenceAnnotatedTypeFactory;
import checkers.inference.InferenceChecker;
import checkers.inference.InferrableChecker;
import checkers.inference.SlotManager;
import checkers.inference.VariableAnnotator;
import checkers.inference.model.ConstraintManager;
import checkers.inference.model.Slot;
import checkers.inference.model.VariableSlot;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.ImplicitsTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import pico.inference.PICORealTypeFactory.PICOPropagationTreeAnnotator;
import pico.typecheck.PICOTypeUtil;

import java.util.Arrays;
import java.util.HashSet;

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
        // Always call postInit() at the end of ATF constructor!
        postInit();
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(super.createTreeAnnotator(), new PICOInferencePropagationTreeAnnotator(this));
    }

    @Override
    protected TypeAnnotator createTypeAnnotator() {
        return new ListTypeAnnotator(super.createTypeAnnotator(), new PICOInferenceImplicitsTypeAnnotator(this));
    }

    // TODO This will be implemented in higher level, as lots of type systems actually don't need the declaration constraint
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

    class PICOInferencePropagationTreeAnnotator extends PropagationTreeAnnotator {
        public PICOInferencePropagationTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        /**Add immutable to the result type of a binary operation if the result type is implicitly immutable*/
        @Override
        public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
            applyImmutableIfImplicitlyImmutable(type);// Usually there isn't existing annotation on binary trees, but to be safe, run it first
            return super.visitBinary(node, type);
        }

        /**Add immutable to the result type of a cast if the result type is implicitly immutable*/
        @Override
        public Void visitTypeCast(TypeCastTree node, AnnotatedTypeMirror type) {
            applyImmutableIfImplicitlyImmutable(type);// Must run before calling super method to respect existing annotation
            return super.visitTypeCast(node, type);
        }

        /**Because TreeAnnotator runs before ImplicitsTypeAnnotator, implicitly immutable types are not guaranteed
         to always have immutable annotation. If this happens, we manually add immutable to type. */
        private void applyImmutableIfImplicitlyImmutable(AnnotatedTypeMirror type) {
            if (PICOTypeUtil.isImplicitlyImmutableType(type)) {
                SlotManager slotManager = PICOInferenceAnnotatedTypeFactory.this.slotManager;
                ConstraintManager constraintManager = PICOInferenceAnnotatedTypeFactory.this.constraintManager;
                Slot shouldBeInferredImmutableVar = slotManager.getVariableSlot(type);
                Slot immutableConstant = slotManager.createConstantSlot(PICOInferenceChecker.IMMUTABLE);
                constraintManager.createEqualityConstraint(shouldBeInferredImmutableVar, immutableConstant);
            }
        }
    }

    class PICOInferenceImplicitsTypeAnnotator extends ImplicitsTypeAnnotator {

        public PICOInferenceImplicitsTypeAnnotator(AnnotatedTypeFactory typeFactory) {
            super(typeFactory);
        }

        /**Also applies implicits to method receiver*/
        @Override
        public Void visitExecutable(AnnotatedExecutableType t, Void p) {
            // There is a TODO in the PICOAnnotatedTypeFactory to investigate. This is just a copy
            super.visitExecutable(t, p);
            // Also scan the receiver to apply implicit annotation
            if (t.getReceiverType() != null) {
                return scanAndReduce(t.getReceiverType(), p, null);
            }
            return null;
        }
    }
}
