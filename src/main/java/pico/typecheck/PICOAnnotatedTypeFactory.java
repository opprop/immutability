package pico.typecheck;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import org.checkerframework.checker.initialization.InitializationAnnotatedTypeFactory;
import org.checkerframework.checker.initialization.qual.FBCBottom;
import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.RelevantJavaTypes;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.QualifierHierarchy;
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
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.framework.util.ViewpointAdapter;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import qual.Bottom;
import qual.Immutable;
import qual.Mutable;
import qual.PolyMutable;
import qual.Readonly;
import qual.ReceiverDependantMutable;
import qual.SubstitutablePolyMutable;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Types;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static pico.typecheck.PICOAnnotationMirrorHolder.COMMITED;
import static pico.typecheck.PICOAnnotationMirrorHolder.IMMUTABLE;
import static pico.typecheck.PICOAnnotationMirrorHolder.MUTABLE;
import static pico.typecheck.PICOAnnotationMirrorHolder.POLY_MUTABLE;
import static pico.typecheck.PICOAnnotationMirrorHolder.READONLY;
import static pico.typecheck.PICOAnnotationMirrorHolder.RECEIVER_DEPENDANT_MUTABLE;
import static pico.typecheck.PICOAnnotationMirrorHolder.SUBSTITUTABLE_POLY_MUTABLE;

/**
 * AnnotatedTypeFactory for PICO. In addition to getting atms, it also propagates and applies mutability
 * qualifiers correctly depending on AST locations(e.g. fields, binary trees) or methods(toString(), hashCode(),
 * clone(), equals(Object o)) using TreeAnnotators and TypeAnnotators. It also applies implicits to method
 * receiver that is not so by default in super implementation.
 */
//TODO Use @Immutable for classes that extends those predefined immutable classess like String or Number
    // and explicitly annotated classes with @Immutable on its declaration
public class PICOAnnotatedTypeFactory extends InitializationAnnotatedTypeFactory<PICOValue,
        PICOStore, PICOTransfer, PICOAnalysis> {

    public PICOAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker, true);
        postInit();
        addAliasedAnnotation(org.jmlspecs.annotation.Readonly.class, READONLY);
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new LinkedHashSet<Class<? extends Annotation>>(
                Arrays.asList(
                        Readonly.class,
                        Mutable.class,
                        PolyMutable.class,
                        ReceiverDependantMutable.class,
                        SubstitutablePolyMutable.class,
                        Immutable.class,
                        Bottom.class,
                        Initialized.class,
                        UnderInitialization.class,
                        UnknownInitialization.class,
                        FBCBottom.class));
    }

    @Override
    protected ViewpointAdapter<?> createViewpointAdapter() {
        return new PICOViewpointAdapter();
    }

    /**Annotators are executed by the added order. Same for Type Annotator*/
    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                new PICOPropagationTreeAnnotator(this),
                new ImplicitsTreeAnnotator(this),
                new CommitmentTreeAnnotator(this),
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
        typeAnnotators.add(new CommitmentTypeAnnotator(this));
        return new ListTypeAnnotator(typeAnnotators);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new PICOQualifierHierarchy(factory, (Object[]) null);
    }

    // Transfer the visibility to package
    @Override
    protected void viewpointAdaptMethod(ExecutableElement methodElt, AnnotatedTypeMirror receiverType, AnnotatedExecutableType methodType) {
        super.viewpointAdaptMethod(methodElt, receiverType, methodType);
    }

    /**Just to transfer the method from super class to package*/
    @Override
    protected boolean isInitializationAnnotation(AnnotationMirror anno) {
        return super.isInitializationAnnotation(anno);
    }

    @Override
    public AnnotationMirror getFieldInvariantAnnotation() {
        return IMMUTABLE;
    }

    /**This affects what fields pico warns not initialized in constructors*/
    @Override
    protected boolean hasFieldInvariantAnnotation(AnnotatedTypeMirror type, VariableElement fieldElement) {
        // This affects which fields should be guaranteed to be initialized
        Set<AnnotationMirror> lowerBounds =
                AnnotatedTypes.findEffectiveLowerBoundAnnotations(qualHierarchy, type);
        return (AnnotationUtils.containsSame(lowerBounds, IMMUTABLE) || AnnotationUtils.containsSame(lowerBounds, RECEIVER_DEPENDANT_MUTABLE))
                && !PICOTypeUtil.isAssignableField(fieldElement, this);
    }

    /**Forbid applying top annotations to type variables if they are used on local variables*/
    @Override
    public boolean getShouldDefaultTypeVarLocals() {
        return false;
    }

    /**This covers the case when static fields are used and constructor is accessed as an element(regarding to
     * applying @Immutable on type declaration to constructor return type).*/
    @Override
    public void addComputedTypeAnnotations(Element elt, AnnotatedTypeMirror type) {
        PICOTypeUtil.addDefaultForStaticField(this, type, elt);
        PICOTypeUtil.applyImmutableToConstructorReturnOfImmutableClass(this, elt, type);
        super.addComputedTypeAnnotations(elt, type);
    }

    @Override
    protected void annotateInheritedFromClass(AnnotatedTypeMirror type, Set<AnnotationMirror> fromClass) {
        // If interitted from class element is @Mutable or @Immutable, then apply this annotation to the usage type
        if (fromClass.contains(MUTABLE) || fromClass.contains(IMMUTABLE)) {
            super.annotateInheritedFromClass(type, fromClass);
            return;
        }
        // If interitted from class element is @ReceiverDependantMutable, then don't apply and wait for @Mutable
        // (default qualifier in hierarchy to be applied to the usage type). This is to avoid having @ReceiverDependantMutable
        // on type usages as a default behaviour. By default, @Mutable is better used as the type for usages that
        // don't have explicit annotation.
        return;// Don't add annotations from class element
    }

    /**This method gets lhs WITH flow sensitive refinement*/
    // TODO Should refactor super class to avoid too much duplicate code.
    // This method is pretty hacky right now.
    @Override
    public AnnotatedTypeMirror getAnnotatedTypeLhs(Tree lhsTree) {
        boolean oldComputingAnnotatedTypeMirrorOfLHS = computingAnnotatedTypeMirrorOfLHS;
        computingAnnotatedTypeMirrorOfLHS = true;

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

        computingAnnotatedTypeMirrorOfLHS = oldComputingAnnotatedTypeMirrorOfLHS;
        return result;
    }

    /**Handles invoking static methods with polymutable on its declaration*/
    @Override
    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> methodFromUse(ExpressionTree tree, ExecutableElement methodElt, AnnotatedTypeMirror receiverType) {
        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> pair = super.methodFromUse(tree, methodElt, receiverType);
        // We want to replace polymutable with substitutablepolymutable when we invoke static methods
        if (ElementUtils.isStatic(methodElt)) {
            AnnotatedExecutableType methodType = pair.first;
            AnnotatedTypeMirror returnType = methodType.getReturnType();
            if (returnType.hasAnnotation(POLY_MUTABLE)) {
                // Only substitute polymutable but not other qualifiers! Missing the if statement
                // caused bugs before!
                returnType.replaceAnnotation(SUBSTITUTABLE_POLY_MUTABLE);
            }
            List<AnnotatedTypeMirror> parameterTypes = methodType.getParameterTypes();
            for (AnnotatedTypeMirror p : parameterTypes) {
                if (returnType.hasAnnotation(POLY_MUTABLE)) {
                    p.replaceAnnotation(SUBSTITUTABLE_POLY_MUTABLE);
                }
            }
        }
        return pair;
    }

    protected class PICOQualifierHierarchy extends InitializationQualifierHierarchy {

        public PICOQualifierHierarchy(MultiGraphFactory f, Object[] arg) {
            super(f, arg);
        }

        @Override
        public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {
            if (isInitializationAnnotation(subAnno) || isInitializationAnnotation(superAnno)) {
                return this.isSubtypeInitialization(subAnno, superAnno);
            }
            return super.isSubtype(subAnno, superAnno);
        }

        @Override
        public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
            if (isInitializationAnnotation(a1) || isInitializationAnnotation(a2)) {
                return this.leastUpperBoundInitialization(a1, a2);
            }
            return super.leastUpperBound(a1, a2);
        }
    }

    /**Tree Annotators*/
    public static class PICOPropagationTreeAnnotator extends PropagationTreeAnnotator {
        public PICOPropagationTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        // TODO This is very ugly. Why is array component type from lhs propagates to rhs?!
        @Override
        public Void visitNewArray(NewArrayTree tree, AnnotatedTypeMirror type) {
            // Below is copied from super
            assert type.getKind() == TypeKind.ARRAY
                    : "PropagationTreeAnnotator.visitNewArray: should be an array type";

            AnnotatedTypeMirror componentType = ((AnnotatedTypeMirror.AnnotatedArrayType) type).getComponentType();

            Collection<? extends AnnotationMirror> prev = null;
            if (tree.getInitializers() != null && tree.getInitializers().size() != 0) {
                // We have initializers, either with or without an array type.

                for (ExpressionTree init : tree.getInitializers()) {
                    AnnotatedTypeMirror initType = atypeFactory.getAnnotatedType(init);
                    // initType might be a typeVariable, so use effectiveAnnotations.
                    Collection<AnnotationMirror> annos = initType.getEffectiveAnnotations();

                    prev = (prev == null) ? annos : atypeFactory.getQualifierHierarchy().leastUpperBounds(prev, annos);
                }
            } else {
                prev = componentType.getAnnotations();
            }

            assert prev != null
                    : "PropagationTreeAnnotator.visitNewArray: violated assumption about qualifiers";

            Pair<Tree, AnnotatedTypeMirror> context =
                    atypeFactory.getVisitorState().getAssignmentContext();
            Collection<? extends AnnotationMirror> post;

            if (context != null
                    && context.second != null
                    && context.second instanceof AnnotatedTypeMirror.AnnotatedArrayType) {
                AnnotatedTypeMirror contextComponentType =
                        ((AnnotatedTypeMirror.AnnotatedArrayType) context.second).getComponentType();
                // Only compare the qualifiers that existed in the array type
                // Defaulting wasn't performed yet, so prev might have fewer qualifiers than
                // contextComponentType, which would cause a failure.
                // TODO: better solution?
                boolean prevIsSubtype = true;
                for (AnnotationMirror am : prev) {
                    if (contextComponentType.isAnnotatedInHierarchy(am)
                            && !atypeFactory.getQualifierHierarchy().isSubtype(
                            am, contextComponentType.getAnnotationInHierarchy(am))) {
                        prevIsSubtype = false;
                    }
                }
                // TODO: checking conformance of component kinds is a basic sanity check
                // It fails for array initializer expressions. Those should be handled nicer.
                if (contextComponentType.getKind() == componentType.getKind()
                        && (prev.isEmpty()
                        || (!contextComponentType.getAnnotations().isEmpty()
                        && prevIsSubtype))) {
                    post = contextComponentType.getAnnotations();
                } else {
                    // The type of the array initializers is incompatible with the
                    // context type!
                    // Somebody else will complain.
                    post = prev;
                }
            } else {
                // No context is available - simply use what we have.
                post = prev;
            }

            // Below line is the only difference from super implementation
            applyImmutableIfImplicitlyImmutable(componentType);
            // Above line is the only difference from super implementation
            componentType.addMissingAnnotations(post);

            return null;
            // Above is copied from super
        }

        /**Add immutable to the result type of a binary operation if the result type is implicitly immutable*/
        @Override
        public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
            applyImmutableIfImplicitlyImmutable(type);// Usually there isn't existing annotation on binary trees, but to be safe, run it first
            super.visitBinary(node, type);
            // NullnessPropagationTreeAnnotator says result type of binary tree is always @Initialized. So replace it
            // with COMMITED here.
            applyCommitedIfSupported(atypeFactory, type);
            return null;
        }

        @Override
        public Void visitUnary(UnaryTree node, AnnotatedTypeMirror type) {
            super.visitUnary(node, type);
            // Same reason as above
            applyCommitedIfSupported(atypeFactory, type);
            return null;
        }

        /**Add immutable to the result type of a cast if the result type is implicitly immutable*/
        @Override
        public Void visitTypeCast(TypeCastTree node, AnnotatedTypeMirror type) {
            applyImmutableIfImplicitlyImmutable(type);// Must run before calling super method to respect existing annotation
            return super.visitTypeCast(node, type);
        }

        /**Because TreeAnnotator runs before ImplicitsTypeAnnotator, implicitly immutable types are not guaranteed
         to always have immutable annotation. If this happens, we manually add immutable to type. We use
         addMissingAnnotations because we want to respect existing annotation on type*/
        private void applyImmutableIfImplicitlyImmutable(AnnotatedTypeMirror type) {
            if (PICOTypeUtil.isImplicitlyImmutableType(type)) {
                type.addMissingAnnotations(new HashSet<>(Arrays.asList(IMMUTABLE)));
            }
        }

        private void applyCommitedIfSupported(AnnotatedTypeFactory annotatedTypeFactory, AnnotatedTypeMirror type) {
            if (annotatedTypeFactory.isSupportedQualifier(COMMITED)) {
                type.replaceAnnotation(COMMITED);
            }
        }
    }

    /**Apply defaults for static fields with non-implicitly immutable types*/
    public static class PICOTreeAnnotator extends TreeAnnotator {
        public PICOTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        // This adds @Immutable annotation to constructor return type if type declaration has @Immutable when the
        // constructor is accessed as a tree.
        @Override
        public Void visitMethod(MethodTree node, AnnotatedTypeMirror p) {
            Element element = TreeUtils.elementFromDeclaration(node);
            // See: https://github.com/opprop/checker-framework/blob/master/framework/src/org/checkerframework/framework/type/AnnotatedTypeFactory.java#L1593
            // for why constructor return is not applied class bound annotation
            PICOTypeUtil.applyImmutableToConstructorReturnOfImmutableClass(atypeFactory, element, p);
            return super.visitMethod(node, p);
        }

        /**This covers the declaration of static fields*/
        @Override
        public Void visitVariable(VariableTree node, AnnotatedTypeMirror annotatedTypeMirror) {
            VariableElement element = TreeUtils.elementFromDeclaration(node);
            PICOTypeUtil.addDefaultForStaticField(atypeFactory, annotatedTypeMirror, element);
            Types types = atypeFactory.getProcessingEnv().getTypeUtils();
            viewpointAdaptInstanceFieldToClassBound(types, annotatedTypeMirror, element, node);
            return super.visitVariable(node, annotatedTypeMirror);
        }

        /**
         * Adapts main modifier of an instance field to the bound of the enclosing class.
         *
         * So if a field declaration has initializer, viewpoint adapted field type is used.
         * But this viewpoint adaptation doesn't affect the type of usage of field in other
         * locations, for example in constructors and initialization blocks: declared type
         * of the field is still used for viewpoint adaptation inside constructors and initialization
         * blocks instead of the adapted field type. So the affecting scope of the viewpoint
         * adaptation is only instance field declarations with initializer on it.
         *
         * If the field type is type variable, it doesn't viewpoint adapt the bounds of the type
         * variable. If there is annotation on that type variable use, this method still adapts
         * that main modifier to the bound of the enclosing class.
         */
        private void viewpointAdaptInstanceFieldToClassBound(
                Types types, AnnotatedTypeMirror annotatedTypeMirror, VariableElement element, VariableTree tree) {
            if (element != null && element.getKind() == ElementKind.FIELD && !ElementUtils.isStatic(element)) {
                AnnotationMirror boundAnnotation = PICOTypeUtil.getBoundAnnotationOnEnclosingTypeDeclaration(tree, atypeFactory);
                if (boundAnnotation == null) return;
                AnnotatedDeclaredType typeDeclaration = atypeFactory.fromElement(ElementUtils.enclosingClass(element));
                // Because boundAnnotation is the result of applying all different cases to determine bound annotation of
                // a type element, so we forcely replace the bound with boundAnnotation. They might be the same sometimes,
                // but we still replace it anyway.
                typeDeclaration.replaceAnnotation(boundAnnotation);
                AnnotatedTypeMirror adaptedFieldType = AnnotatedTypes.asMemberOf(types, atypeFactory, typeDeclaration, element, tree);
                // Type variable use with no annotation on its main modifier hits null case
                if (adaptedFieldType.getAnnotationInHierarchy(READONLY) != null) {
                    // Possible cases: AnnotatedDeclaredType, AnnotatedArrayType or AnnotatedTypeVariable with annotation on it
                    annotatedTypeMirror.replaceAnnotation(adaptedFieldType.getAnnotationInHierarchy(READONLY));
                }
            }
        }
    }

    /**Type Annotators*/
    public static class PICOTypeAnnotator extends TypeAnnotator {

        public PICOTypeAnnotator(AnnotatedTypeFactory typeFactory) {
            super(typeFactory);
        }

        /**Applies pre-knowledged defaults that are same with jdk.astub to toString, hashCode, equals,
           clone Object methods*/
        @Override
        public Void visitExecutable(AnnotatedExecutableType t, Void p) {
            super.visitExecutable(t, p);

            // Only handle instance methods, not static methods
            if (!ElementUtils.isStatic(t.getElement())) {
                if (PICOTypeUtil.isMethodOrOverridingMethod(t, "toString()", typeFactory)
                        || PICOTypeUtil.isMethodOrOverridingMethod(t, "hashCode()", typeFactory)) {
                    t.getReceiverType().addMissingAnnotations(new HashSet<>(Arrays.asList(READONLY)));
                } else if (PICOTypeUtil.isMethodOrOverridingMethod(t, "equals(java.lang.Object)", typeFactory)) {
                    t.getReceiverType().addMissingAnnotations(new HashSet<>(Arrays.asList(READONLY)));
                    t.getParameterTypes().get(0).addMissingAnnotations(new HashSet<>(Arrays.asList(READONLY)));
                }
            }

            return null;
        }

    }

    public static class PICOImplicitsTypeAnnotator extends ImplicitsTypeAnnotator {

        public PICOImplicitsTypeAnnotator(AnnotatedTypeFactory typeFactory) {
            super(typeFactory);
        }

        /**Also applies implicits to method receiver*/
        @Override
        public Void visitExecutable(AnnotatedExecutableType t, Void p) {
            // TODO The implementation before doesn't work after update. Previously, I sanned the
            // method receiver without null check. But even if I check nullness, scanning receiver
            // at first caused some tests to fail. Need to investigate the reason.
            super.visitExecutable(t, p);
            // Also scan the receiver to apply implicit annotation
            if (t.getReceiverType() != null) {
                return scanAndReduce(t.getReceiverType(), p, null);
            }
            return null;
        }
    }

    // TODO Right now, instance method receiver cannot inherit bound annotation from class element, and
    // this caused the inconsistency when accessing the type of receiver while visiting the method and
    // while visiting the variable tree. Implicit annotation can be inserted to method receiver via
    // extending ImplicitsTypeAnnotator; But InheritedFromClassAnnotator cannot be inheritted because its
    // constructor is private and I can't override it to also inherit bound annotation from class element
    // to the declared receiver type of instance methods. To view the details, look at ImmutableClass1.java
    // testcase.
    // class PICOInheritedFromClassAnnotator extends InheritedFromClassAnnotator {}
}
