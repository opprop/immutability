package pico.inference;

import checkers.inference.InferenceAnnotatedTypeFactory;
import checkers.inference.InferenceChecker;
import checkers.inference.InferenceTreeAnnotator;
import checkers.inference.InferrableChecker;
import checkers.inference.SlotManager;
import checkers.inference.VariableAnnotator;
import checkers.inference.model.ConstantSlot;
import checkers.inference.model.ConstraintManager;
import checkers.inference.model.Slot;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.ImplicitsTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import pico.typecheck.PICOAnnotatedTypeFactory.PICOImplicitsTypeAnnotator;
import pico.typecheck.PICOTypeUtil;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Types;

import static pico.typecheck.PICOAnnotationMirrorHolder.IMMUTABLE;

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

    // Having PICOInferencePropagationTreeAnnotator before InferenceTreeAnnotator makes the inference behaviour
    // consistent with typechecking side: it will have untouched BinaryTrees and TypeCastTrees, with no VarAnnot.
    // as input. InferenceTreeAnnotator internally uses VariableAnnotator to insert VarAnnot to all kinds of tress
    // including BinaryTrees and TypeCastTrees. So if InferenceTreeAnnotator is added before PICOInferencePropagationTreeAnnotator,
    // it inserts VarAnnot to those two locations and solution will be inserted back to type case location(binary tree always can't
    // be inserted results anyway).
    @Override
    public TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(new ImplicitsTreeAnnotator(this),
                new PICOInferencePropagationTreeAnnotator(this),
                new PICOInferenceTreeAnnotator(this, realChecker, realTypeFactory, variableAnnotator, slotManager));
    }

    @Override
    protected TypeAnnotator createTypeAnnotator() {
        // Reuse PICOImplicitsTypeAnnotator even in inference mode. Because the type annotator's implementation
        // are the same. The only drawback is that naming is not good(doesn't include "Inference"), thus may be
        // hard to debug
        return new ListTypeAnnotator(super.createTypeAnnotator(), new PICOImplicitsTypeAnnotator(this));
    }

    // TODO This will be implemented in higher level, as lots of type systems actually don't need the declaration constraint
    @Override
    public VariableAnnotator createVariableAnnotator() {
        return new PICOVariableAnnotator(this, realTypeFactory, realChecker, slotManager, constraintManager);
    }

    @Override
    protected void viewpointAdaptMethod(ExecutableElement methodElt, AnnotatedTypeMirror receiverType, AnnotatedTypeMirror.AnnotatedExecutableType methodType) {
        super.viewpointAdaptMethod(methodElt, receiverType, methodType);
    }

    private void applyConstant(AnnotatedTypeMirror type, AnnotationMirror am) {
        SlotManager slotManager = PICOInferenceAnnotatedTypeFactory.this.slotManager;
        ConstraintManager constraintManager = PICOInferenceAnnotatedTypeFactory.this.constraintManager;
        // Might be null. It's normal. In typechecking side, we use addMissingAnnotations(). Only if
        // there is existing annotation in code, then here is non-null. Otherwise, VariableAnnotator
        // hasn't come into the picture yet, so no VarAnnot exists here, which is normal.
        Slot shouldBeAppliedTo = slotManager.getVariableSlot(type);
        ConstantSlot constant = slotManager.createConstantSlot(am);
        if (shouldBeAppliedTo == null) {
            // Here, we are adding VarAnnot that represents @Immutable. There won't be solution for this ConstantSlot for this type,
            // so the inserted-back source code doesn't have explicit annotation @Immutable. But it is not wrong. It makes the code
            // cleaner by omitting implicit annotations. General principle is that for ConstantSlot, there won't be annotation inserted
            // back to the original source code, BUT this ConstantSlot(representing @Immutable) will be used for constraint generation
            // that affects the solutions for other VariableSlots
            type.addAnnotation(slotManager.getAnnotation(constant));// Insert Constant VarAnnot that represents @Immutable
            type.addAnnotation(am);// Insert real @Immutable. This should be removed if INF-FR only uses VarAnnot
        } else {
            constraintManager.addEqualityConstraint(shouldBeAppliedTo, constant);
        }
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
                applyConstant(type, IMMUTABLE);
            }
        }
    }

    static class PICOInferenceTreeAnnotator extends InferenceTreeAnnotator {


        public PICOInferenceTreeAnnotator(InferenceAnnotatedTypeFactory atypeFactory, InferrableChecker realChecker,
                                          AnnotatedTypeFactory realAnnotatedTypeFactory, VariableAnnotator variableAnnotator, SlotManager slotManager) {
            super(atypeFactory, realChecker, realAnnotatedTypeFactory, variableAnnotator, slotManager);
        }

        // Viewpoint adapt field declaration type to class bound, and replace main modifier with
        // the adaptation result
        @Override
        public Void visitVariable(VariableTree node, AnnotatedTypeMirror annotatedTypeMirror) {
            // In inference side, must call super first to store corresponding element of node into
            // VariableAnntator to avoid infinite loop
            super.visitVariable(node, annotatedTypeMirror);
            VariableElement element = TreeUtils.elementFromDeclaration(node);
            Types types = atypeFactory.getProcessingEnv().getTypeUtils();
            viewpointAdaptInstanceFieldToClassBound(types, annotatedTypeMirror, element, node);
            return null;
        }

        private void viewpointAdaptInstanceFieldToClassBound(
                Types types, AnnotatedTypeMirror annotatedTypeMirror, VariableElement element, VariableTree tree) {
            if (element != null && element.getKind() == ElementKind.FIELD && !ElementUtils.isStatic(element)) {
                AnnotatedTypeMirror.AnnotatedDeclaredType bound = PICOTypeUtil.getBoundTypeOfEnclosingTypeDeclaration(tree, atypeFactory);
                AnnotatedTypeMirror adaptedFieldType = AnnotatedTypes.asMemberOf(types, atypeFactory, bound, element, tree);
                // Type variable use with no annotation on its main modifier hits null case
                // The replaced annotation only affects solutions for other slots, but doesn't change the solution
                // for the variable tree itself, which is expected behaviour.
                annotatedTypeMirror.replaceAnnotations(adaptedFieldType.getAnnotations());
            }
        }
    }
}
