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
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.util.TreePath;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.treeannotator.ImplicitsTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.javacutil.TreeUtils;
import pico.typecheck.PICOAnnotatedTypeFactory.PICOImplicitsTypeAnnotator;
import pico.typecheck.PICOTypeUtil;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

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
                new InferenceTreeAnnotator(this, realChecker, realTypeFactory, variableAnnotator, slotManager));
    }

    @Override
    protected TypeAnnotator createTypeAnnotator() {
        // Reuse PICOImplicitsTypeAnnotator even in inference mode. Because the type annotator's implementation
        // are the same. The only drawback is that naming is not good(doesn't include "Inference"), thus may be
        // hard to debug
        return new ListTypeAnnotator(super.createTypeAnnotator(), new PICOImplicitsTypeAnnotator(this));
    }

    @Override
    public VariableAnnotator createVariableAnnotator() {
        return new PICOVariableAnnotator(this, realTypeFactory, realChecker, slotManager, constraintManager);
    }

    @Override
    protected void viewpointAdaptMember(AnnotatedTypeMirror type, AnnotatedTypeMirror owner, Element element) {
        super.viewpointAdaptMember(type, owner, element);
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

    /**
     * Gets self type from a tree.
     *
     * This method doesn't flush and modify "type" if "methodReceiver" is non-null, compared to
     * its super implementation. In inferene mode, modifying type directly is dangerous, because
     * type is singleton, so it mappes to one unique element. If the type associated is mutated,
     * other clients later on will all see this change, which is not expected behaviour.
     *
     * @param tree tree from which self type is being extracted
     * @return self type of tree
     */
    public AnnotatedDeclaredType getSelfType(Tree tree) {
        TreePath path = getPath(tree);
        ClassTree enclosingClass = TreeUtils.enclosingClass(path);
        if (enclosingClass == null) {
            // I hope this only happens when tree is a fake tree that
            // we created, e.g. when desugaring enhanced-for-loops.
            enclosingClass = getCurrentClassTree(tree);
        }
        // "type" is right now VarAnnot inserted to the bound of "enclosingClass"
        AnnotatedDeclaredType type = getAnnotatedType(enclosingClass);

        MethodTree enclosingMethod = TreeUtils.enclosingMethod(path);
        if (enclosingClass.getSimpleName().length() != 0 && enclosingMethod != null) {
            AnnotatedTypeMirror.AnnotatedDeclaredType methodReceiver;
            if (TreeUtils.isConstructor(enclosingMethod)) {
                methodReceiver =
                        (AnnotatedTypeMirror.AnnotatedDeclaredType) getAnnotatedType(enclosingMethod).getReturnType();
            } else {
                methodReceiver = getAnnotatedType(enclosingMethod).getReceiverType();
            }
            // Directly return "methodReceiver" instead of clearing "type" and copying its annotations to "type"
            if (methodReceiver != null) {
                return methodReceiver;
            }
        }
        // Only by this, bound VarAnnot isn't side-effectively altered to be replaced with annotations from "methodReceiver"
        return type;
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
}
