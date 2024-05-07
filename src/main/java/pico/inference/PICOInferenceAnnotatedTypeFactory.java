package pico.inference;

import checkers.inference.InferenceAnnotatedTypeFactory;
import checkers.inference.InferenceChecker;
import checkers.inference.InferenceMain;
import checkers.inference.InferenceQualifierHierarchy;
import checkers.inference.InferenceTreeAnnotator;
import checkers.inference.InferrableChecker;
import checkers.inference.SlotManager;
import checkers.inference.VariableAnnotator;
import checkers.inference.model.ConstraintManager;
import checkers.inference.model.Slot;
import checkers.inference.model.SourceVariableSlot;
import checkers.inference.util.InferenceViewpointAdapter;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.util.TreePath;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.framework.type.*;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.treeannotator.LiteralTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.javacutil.*;
import pico.common.ExtendedViewpointAdapter;
import pico.common.ViewpointAdapterGettable;
import pico.common.PICOTypeUtil;
import pico.typecheck.PICONoInitAnnotatedTypeFactory;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static pico.typecheck.PICOAnnotationMirrorHolder.*;
import static pico.typecheck.PICOAnnotationMirrorHolder.RECEIVER_DEPENDANT_MUTABLE;

/**
 * Propagates correct constraints on trees and types using TreeAnnotators and TypeAnnotators.
 * Add preference constraint on defaulting behaviour in typecheck mode, as the annotation can
 * be something different;
 * Replace VariableSlot that represents the annotation with ConstantSlot that is the implicit
 * type on that type. This ensures that that VariableSlot doesn't enter solver and solver doesn't
 * give solution to the VariableSlot, and there won't be annotations inserted to implicit locations.
 */
public class PICOInferenceAnnotatedTypeFactory extends InferenceAnnotatedTypeFactory implements ViewpointAdapterGettable {
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
        return new ListTreeAnnotator(new LiteralTreeAnnotator(this),
                new PICOInferencePropagationTreeAnnotator(this),
                new InferenceTreeAnnotator(this, realChecker, realTypeFactory, variableAnnotator, slotManager));
    }

    @Override
    protected TypeAnnotator createTypeAnnotator() {
        // Reuse PICODefaultForTypeAnnotator even in inference mode. Because the type annotator's implementation
        // are the same. The only drawback is that naming is not good(doesn't include "Inference"), thus may be
        // hard to debug
        return new ListTypeAnnotator(super.createTypeAnnotator(), new PICONoInitAnnotatedTypeFactory.PICODefaultForTypeAnnotator(this));
    }

    @Override
    public VariableAnnotator createVariableAnnotator() {
        return new PICOVariableAnnotator(this, realTypeFactory, realChecker, slotManager, constraintManager);
    }

    @Override
    protected InferenceViewpointAdapter createViewpointAdapter() {
        return new PICOInferenceViewpointAdapter(this);
    }

    VariableAnnotator getVariableAnnotator() {
        return variableAnnotator;
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
        ClassTree enclosingClass = TreePathUtil.enclosingClass(path);
        if (enclosingClass == null) {
            // I hope this only happens when tree is a fake tree that
            // we created, e.g. when desugaring enhanced-for-loops.
            enclosingClass = TreePathUtil.enclosingClass(getPath(tree));
        }
        // "type" is right now VarAnnot inserted to the bound of "enclosingClass"
        AnnotatedDeclaredType type = getAnnotatedType(enclosingClass);

        MethodTree enclosingMethod = TreePathUtil.enclosingMethod(path);
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

    @Override
    public ExtendedViewpointAdapter getViewpointAdapter() {
        return (ExtendedViewpointAdapter) viewpointAdapter;
    }

    @Override
    protected AnnotationMirrorSet getDefaultTypeDeclarationBounds() {
        return new AnnotationMirrorSet(MUTABLE);
    }

    @Override
    protected QualifierHierarchy createQualifierHierarchy() {
        return new PICOInferenceQualifierHierarchy(getSupportedTypeQualifiers(), elements, this);
    }

    @Override
    public AnnotationMirrorSet getTypeDeclarationBounds(TypeMirror type) {
        // Get the VarAnnot on the class decl
        // This factory is only invoked on inference, so no need to provide concrete anno for type-check
        if (type instanceof PrimitiveType) {
            return new AnnotationMirrorSet(slotManager.getAnnotation(slotManager.getSlot(IMMUTABLE)));
        }
        if (type.getKind() == TypeKind.ARRAY) {
            // WORKAROUND: return RDM will cause issues with new clauses
            return new AnnotationMirrorSet(slotManager.getAnnotation(slotManager.getSlot(READONLY)));
        }

        if (PICOTypeUtil.isEnumOrEnumConstant(type)) {
            return new AnnotationMirrorSet(slotManager.getAnnotation(slotManager.getSlot(IMMUTABLE)));
        }
//        AnnotatedTypeMirror atm = toAnnotatedType(type, true);
//        if (atm instanceof AnnotatedDeclaredType && ((AnnotatedDeclaredType) atm).getTypeArguments().size() > 0) {
//            // Workaround for types with type arguments.
//            // annotateElementFromStore can only get the original type with type param.
//            // But this method only needs the top annotation.
//            // Note: class bound cache is a private field of annotator.
//
//            atm = PICOTypeUtil.getBoundTypeOfTypeDeclaration(TypesUtils.getTypeElement(type), this);
//        } else {
//            getVariableAnnotator().annotateElementFromStore(getProcessingEnv().getTypeUtils().asElement(type), atm);
//        }
//
//        if (atm.hasAnnotation(VarAnnot.class)) {
//            return atm.getAnnotations();
//        }
        AnnotationMirror am = ((PICOVariableAnnotator) variableAnnotator).getClassDeclAnno(getProcessingEnv().getTypeUtils().asElement(type));
        if (am != null) {
            return new AnnotationMirrorSet(am);
        }

        // if reaching this point and still no anno: not annotated from slot manager
        // maybe should read from stub file.
        // if implicit: return immutable slot

        // implicit
        if (PICOTypeUtil.isImplicitlyImmutableType(toAnnotatedType(type, false))) {
            return new AnnotationMirrorSet(slotManager.getAnnotation(slotManager.getSlot(IMMUTABLE)));
        }

        // get stub & default from element.
        AnnotatedTypeMirror atm = stubTypes.getAnnotatedTypeMirror(getProcessingEnv().getTypeUtils().asElement(type));
        if (atm != null) {
            Set<AnnotationMirror> set = atm.getAnnotations();
            if (!set.isEmpty()) {
                return new AnnotationMirrorSet(
                        slotManager.getAnnotation(slotManager.getSlot(set.iterator().next())));
            }
        }
        return new AnnotationMirrorSet(
                slotManager.getAnnotation(slotManager.getSlot(super.getTypeDeclarationBounds(type).iterator().next())));
    }

    class PICOInferencePropagationTreeAnnotator extends PropagationTreeAnnotator {
        public PICOInferencePropagationTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree methodInvocationTree, AnnotatedTypeMirror mirror) {
            // This is a workaround for implicit types.
            // Implicit types in lib method get defaulted to mutable.
            // Implicit immutable classes cannot be annotated in stub files, annotations were ignored.
            // Find the cause, annotate implicit immutable classes in stub, and remove this method.
            applyImmutableIfImplicitlyImmutable(mirror);
            return super.visitMethodInvocation(methodInvocationTree, mirror);
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
            if (type.hasAnnotationInHierarchy(READONLY)) {
                // VarAnnot is guarenteed to not exist on type, because PropagationTreeAnnotator has the highest previledge
                // So VarAnnot hasn't been inserted to cast type yet.
                PICOTypeUtil.applyConstant(type, type.getAnnotationInHierarchy(READONLY));
            }
            return super.visitTypeCast(node, type);
        }

        /**Because TreeAnnotator runs before DefaultForTypeAnnotator, implicitly immutable types are not guaranteed
         to always have immutable annotation. If this happens, we manually add immutable to type. */
        private void applyImmutableIfImplicitlyImmutable(AnnotatedTypeMirror type) {
            if (PICOTypeUtil.isImplicitlyImmutableType(type)) {
                PICOTypeUtil.applyConstant(type, IMMUTABLE);
            }
        }
    }

    protected class PICOInferenceQualifierHierarchy extends InferenceQualifierHierarchy {

        public PICOInferenceQualifierHierarchy(
                Set<Class<? extends Annotation>> qualifierClasses,
                Elements elements,
                GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory) {
            super(qualifierClasses, elements, atypeFactory);
        }

        @Override
        public boolean isSubtypeQualifiers(final AnnotationMirror subtype, final AnnotationMirror supertype) {

            if (subtype == null || supertype == null || !isVarAnnot(subtype) || !isVarAnnot(supertype)) {
                if (InferenceMain.isHackMode()) {
                    return true;
                } else {
                    throw new BugInCF("Unexpected arguments for isSubtype: subtype=%s, supertype=%s", subtype, supertype);
                }
            }

            if (supertype.getElementValues().isEmpty()) {
                return true;
            }

            final Slot subSlot   = slotManager.getSlot(subtype);
            final Slot superSlot = slotManager.getSlot(supertype);

            if (superSlot.getId() == 6) { // don't generate the constraint when it's RDM.
                return true;
            }

            return constraintManager.addSubtypeConstraintNoErrorMsg(subSlot, superSlot);
        }
    }
}
