package pico.inference;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.RelevantJavaTypes;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.treeannotator.ImplicitsTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.ImplicitsTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.IrrelevantTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.PropagationTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.ViewpointAdapter;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.TreeUtils;
import pico.typecheck.PICOTypeUtil;
import qual.Bottom;
import qual.Immutable;
import qual.Mutable;
import qual.Readonly;
import qual.ReceiverDependantMutable;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * PICORealTypeFactory exists because: 1)PICOAnnotatedTypeFactory is not subtype of
 * BaseAnnotatedTypeFactory. 2) In inference side, PICO only supports reduced version of
 * mutability qualifiers. 3) In inference side, PICO doesn't need to care initialization hierarchy.
 * We have all the logic that are in PICOAnnotatedTypeFactory except those that belong
 * to InitializationAnnotatedTypeFactory as if there is only one mutability qualifier hierarchy.
 * This class has lots of copied code from PICOAnnotatedTypeFactory. The two should be in sync.
 */
public class PICORealTypeFactory extends BaseAnnotatedTypeFactory {

    public final AnnotationMirror READONLY, MUTABLE, RECEIVERDEPENDANTMUTABLE, IMMUTABLE, BOTTOM;

    public PICORealTypeFactory(BaseTypeChecker checker, boolean useFlow) {
        super(checker, useFlow);
        READONLY = AnnotationUtils.fromClass(elements, Readonly.class);
        MUTABLE = AnnotationUtils.fromClass(elements, Mutable.class);
        RECEIVERDEPENDANTMUTABLE = AnnotationUtils.fromClass(elements, ReceiverDependantMutable.class);
        IMMUTABLE = AnnotationUtils.fromClass(elements, Immutable.class);
        BOTTOM = AnnotationUtils.fromClass(elements, Bottom.class);

        addAliasedAnnotation(org.jmlspecs.annotation.Readonly.class, READONLY);
        postInit();
    }

    /**Only support mutability qualifier hierarchy*/
    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new LinkedHashSet<Class<? extends Annotation>>(
                Arrays.asList(
                        Readonly.class,
                        Mutable.class,
                        ReceiverDependantMutable.class,
                        Immutable.class,
                        Bottom.class));
    }

    // TODO Remove this temporary viewpoint adaptor
    @Override
    protected ViewpointAdapter<?> createViewpointAdapter() {
        return new PICORealViewpointAdapter();
    }

    /**Annotators are executed by the added order. Same for Type Annotator*/
    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                new PICOPropagationTreeAnnotator(this),
                new ImplicitsTreeAnnotator(this),
                new PICOTreeAnnotator(this));
    }

    // TODO Refactor super class to remove this duplicate code
    @Override
    protected TypeAnnotator createTypeAnnotator() {
        /*Copied code start*/
        List<TypeAnnotator> typeAnnotators = new ArrayList<>();
        RelevantJavaTypes relevantJavaTypes =
                checker.getClass().getAnnotation(RelevantJavaTypes.class);
        if (relevantJavaTypes != null) {
            Class<?>[] classes = relevantJavaTypes.value();
            // Must be first in order to annotated all irrelevant types that are not explicilty
            // annotated.
            typeAnnotators.add(
                    new IrrelevantTypeAnnotator(
                            this, getQualifierHierarchy().getTopAnnotations(), classes));
        }
        typeAnnotators.add(new PropagationTypeAnnotator(this));
        /*Copied code ends*/
        // Adding order is important here. Because internally type annotators are using addMissingAnnotations()
        // method, so if one annotator already applied the annotations, the others won't apply twice at the
        // same location
        typeAnnotators.add(new PICOTypeAnnotator(this));
        typeAnnotators.add(new PICOImplicitsTypeAnnotator(this));
        return new ListTypeAnnotator(typeAnnotators);
    }

    /** TODO If the dataflow refines the type as bottom, should we allow such a refinement? If we allow it,
     PICOValidator will give an error if it begins to enforce @Bottom is not used*/
/*    @Override
    protected void applyInferredAnnotations(AnnotatedTypeMirror type, PICOValue as) {
        super.applyInferredAnnotations(type, as);
        // What to do if refined type is bottom?
    }*/

    /**Forbid applying top annotations to type variables if they are used on local variables*/
    @Override
    public boolean getShouldDefaultTypeVarLocals() {
        return false;
    }

    /**This covers the case when static fields are used*/
    @Override
    public void addComputedTypeAnnotations(Element elt, AnnotatedTypeMirror type) {
        addDefaultForStaticField(type, elt);
        super.addComputedTypeAnnotations(elt, type);
    }

    /**Only apply mutable default to static fields with non-implicitly immutable types. Those are handled
     by the PICOImplicitsTypeAnnotator*/
    private void addDefaultForStaticField(AnnotatedTypeMirror annotatedTypeMirror, Element element) {
        if (element != null && element.getKind() == ElementKind.FIELD && ElementUtils.isStatic(element)) {
            AnnotatedTypeMirror explicitATM = fromElement(element);
            if (!explicitATM.isAnnotatedInHierarchy(READONLY)) {
                if (!PICOTypeUtil.isImplicitlyImmutableType(explicitATM)) {
                    annotatedTypeMirror.replaceAnnotation(MUTABLE);
                }
            }
        }
    }

    /**This method gets lhs WITH flow sensitive refinement*/
    // TODO Should refactor super class to avoid too much duplicate code.
    @Override
    public AnnotatedTypeMirror getAnnotatedTypeLhs(Tree lhsTree) {
        AnnotatedTypeMirror result = null;
        boolean oldShouldCache = shouldCache;
        // Don't cache the result because getAnnotatedType(lhsTree) could
        // be called from elsewhere and would expect flow-sensitive type refinements.
        shouldCache = false;
        switch (lhsTree.getKind()) {
            case VARIABLE:
            case IDENTIFIER:
            case MEMBER_SELECT:
            case ARRAY_ACCESS:
                result = getAnnotatedType(lhsTree);
                break;
            default:
                if (TreeUtils.isTypeTree(lhsTree)) {
                    // lhsTree is a type tree at the pseudo assignment of a returned expression to declared return type.
                    result = getAnnotatedType(lhsTree);
                } else {
                    ErrorReporter.errorAbort(
                            "GenericAnnotatedTypeFactory: Unexpected tree passed to getAnnotatedTypeLhs. "
                                    + "lhsTree: "
                                    + lhsTree
                                    + " Tree.Kind: "
                                    + lhsTree.getKind());
                }
        }
        shouldCache = oldShouldCache;

        return result;
    }

    /**Because TreeAnnotator runs before ImplicitsTypeAnnotator, implicitly immutable types are not guaranteed
     to always have immutable annotation. If this happens, we manually add immutable to type. We use
     addMissingAnnotations because we want to respect existing annotation on type*/
    private void applyImmutableIfImplicitlyImmutable(AnnotatedTypeMirror type) {
        if (PICOTypeUtil.isImplicitlyImmutableType(type)) {
            type.addMissingAnnotations(new HashSet<>(Arrays.asList(IMMUTABLE)));
        }
    }

    /**Tree Annotators*/
    class PICOPropagationTreeAnnotator extends PropagationTreeAnnotator {
        public PICOPropagationTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
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
    }

    /**Apply defaults for static fields with non-implicitly immutable types*/
    class PICOTreeAnnotator extends TreeAnnotator {
        public PICOTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        /**This covers the declaration of static fields*/
        @Override
        public Void visitVariable(VariableTree node, AnnotatedTypeMirror annotatedTypeMirror) {
            VariableElement element = TreeUtils.elementFromDeclaration(node);
            addDefaultForStaticField(annotatedTypeMirror, element);
            return super.visitVariable(node, annotatedTypeMirror);
        }
    }

    /**Type Annotators*/
    class PICOTypeAnnotator extends TypeAnnotator {

        public PICOTypeAnnotator(AnnotatedTypeFactory typeFactory) {
            super(typeFactory);
        }

        /**Applies pre-knowledged defaults that are same with jdk.astub to toString, hashCode, equals,
         clone Object methods*/
        @Override
        public Void visitExecutable(AnnotatedExecutableType t, Void p) {
            super.visitExecutable(t, p);
            if (isMethodOrOverridingMethod(t, "toString()") || isMethodOrOverridingMethod(t, "hashCode()")) {
                t.getReceiverType().addMissingAnnotations(new HashSet<>(Arrays.asList(READONLY)));
            } else if (isMethodOrOverridingMethod(t, "equals(java.lang.Object)")) {
                t.getReceiverType().addMissingAnnotations(new HashSet<>(Arrays.asList(READONLY)));
                t.getParameterTypes().get(0).addMissingAnnotations(new HashSet<>(Arrays.asList(READONLY)));
            } else if (isMethodOrOverridingMethod(t, "clone()")) {
                t.getReceiverType().addMissingAnnotations(new HashSet<>(Arrays.asList(RECEIVERDEPENDANTMUTABLE)));
                t.getReturnType().addMissingAnnotations(new HashSet<>(Arrays.asList(RECEIVERDEPENDANTMUTABLE)));
            }
            return null;
        }

        /**Helper method to determine a method using method name*/
        private boolean isMethodOrOverridingMethod(AnnotatedExecutableType methodType, String methodName) {
            // Check if it is the target method
            if (methodType.getElement().toString().contentEquals(methodName)) return true;
            // Check if it is overriding the target method
            // Because AnnotatedTypes.overriddenMethods returns all the methods overriden in the class hierarchy, we need to
            // iterate over the set to check if it's overriding corresponding methods specifically in java.lang.Object class
            Iterator<Entry<AnnotatedDeclaredType, ExecutableElement>> overriddenMethods
                    = AnnotatedTypes.overriddenMethods(elements, typeFactory, methodType.getElement()).entrySet().iterator();
            while (overriddenMethods.hasNext()) {
                if (overriddenMethods.next().getValue().toString().contentEquals(methodName)) {
                    return true;
                }
            }
            return false;
        }
    }

    class PICOImplicitsTypeAnnotator extends ImplicitsTypeAnnotator {

        public PICOImplicitsTypeAnnotator(AnnotatedTypeFactory typeFactory) {
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
