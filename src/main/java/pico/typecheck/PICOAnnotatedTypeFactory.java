package pico.typecheck;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
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
import org.checkerframework.javacutil.AnnotationBuilder;
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
 * AnnotatedTypeFactory for PICO. In addition to getting atms, it also propagates and applies mutability
 * qualifiers correctly depending on AST locations(e.g. fields, binary trees) or methods(toString(), hashCode(),
 * clone(), equals(Object o)) using TreeAnnotators and TypeAnnotators. It also applies implicits to method
 * receiver that is not so by default in super implementation.
 */
//TODO Use @Immutable for classes that extends those predefined immutable classess like String or Number
    // and explicitly annotated classes with @Immutable on its declaration
public class PICOAnnotatedTypeFactory extends InitializationAnnotatedTypeFactory<PICOValue,
        PICOStore, PICOTransfer, PICOAnalysis> {

    public final AnnotationMirror READONLY, MUTABLE, POLYMUTABLE
    , RECEIVERDEPENDANTMUTABLE, SUBSTITUTABLEPOLYMUTABLE, IMMUTABLE, BOTTOM, COMMITED;

    boolean enableFlow = true;

    public PICOAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker, true);
        READONLY = AnnotationBuilder.fromClass(elements, Readonly.class);
        MUTABLE = AnnotationBuilder.fromClass(elements, Mutable.class);
        POLYMUTABLE = AnnotationBuilder.fromClass(elements, PolyMutable.class);
        RECEIVERDEPENDANTMUTABLE = AnnotationBuilder.fromClass(elements, ReceiverDependantMutable.class);
        SUBSTITUTABLEPOLYMUTABLE = AnnotationBuilder.fromClass(elements, SubstitutablePolyMutable.class);
        IMMUTABLE = AnnotationBuilder.fromClass(elements, Immutable.class);
        BOTTOM = AnnotationBuilder.fromClass(elements, Bottom.class);

        COMMITED = AnnotationBuilder.fromClass(elements, Initialized.class);
        addAliasedAnnotation(org.jmlspecs.annotation.Readonly.class, READONLY);
        postInit();
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
        return (AnnotationUtils.containsSame(lowerBounds, IMMUTABLE) || AnnotationUtils.containsSame(lowerBounds, RECEIVERDEPENDANTMUTABLE))
                && !((PICOVisitor)checker.getVisitor()).isAssignableField(fieldElement);
    }

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

    @Override
    protected void addComputedTypeAnnotations(Tree tree, AnnotatedTypeMirror type, boolean iUseFlow) {
        assert root != null
                : "GenericAnnotatedTypeFactory.addComputedTypeAnnotations: "
                + " root needs to be set when used on trees; factory: "
                + this.getClass();

        treeAnnotator.visit(tree, type);
        typeAnnotator.visit(type, null);
        defaults.annotate(tree, type);

        // Only different from super is: there is a enableFlow to enable/disable flow sensitive refinement
        if (enableFlow && iUseFlow) {
            PICOValue as = getInferredValueFor(tree);

            if (as != null) {
                applyInferredAnnotations(type, as);
            }
        }
    }

    public AnnotatedTypeMirror getReceiverTypeWithNoFlowRefinement(ExpressionTree expression) {
        boolean oldEnableFlow = enableFlow;
        enableFlow = false;
        AnnotatedTypeMirror result = super.getReceiverType(expression);
        enableFlow = oldEnableFlow;
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
            if (returnType.hasAnnotation(POLYMUTABLE)) {
                // Only substitute polymutable but not other qualifiers! Missing the if statement
                // caused bugs before!
                returnType.replaceAnnotation(SUBSTITUTABLEPOLYMUTABLE);
            }
            List<AnnotatedTypeMirror> parameterTypes = methodType.getParameterTypes();
            for (AnnotatedTypeMirror p : parameterTypes) {
                if (returnType.hasAnnotation(POLYMUTABLE)) {
                    p.replaceAnnotation(SUBSTITUTABLEPOLYMUTABLE);
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
    class PICOPropagationTreeAnnotator extends PropagationTreeAnnotator {
        public PICOPropagationTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        /**Add immutable to the result type of a binary operation if the result type is implicitly immutable*/
        @Override
        public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
            applyImmutableIfImplicitlyImmutable(type);// Usually there isn't existing annotation on binary trees, but to be safe, run it first
            super.visitBinary(node, type);
            // NullnessPropagationTreeAnnotator says result type of binary tree is always @Initialized. So replace it
            // with COMMITED here.
            type.replaceAnnotation(COMMITED);
            return null;
        }

        @Override
        public Void visitUnary(UnaryTree node, AnnotatedTypeMirror type) {
            super.visitUnary(node, type);
            // Same reason as above
            type.replaceAnnotation(COMMITED);
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
            viewpointAdaptInstanceFieldToClassBound(annotatedTypeMirror, element, node);
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
                AnnotatedTypeMirror annotatedTypeMirror, VariableElement element, VariableTree tree) {
            if (element != null && element.getKind() == ElementKind.FIELD && !ElementUtils.isStatic(element)) {
                AnnotationMirror boundAnnotation = PICOTypeUtil.getBoundAnnotationOnEnclosingTypeDeclaration(tree, (PICOAnnotatedTypeFactory) atypeFactory);
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
    class PICOTypeAnnotator extends TypeAnnotator {

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
                if (isMethodOrOverridingMethod(t, "toString()") || isMethodOrOverridingMethod(t, "hashCode()")) {
                    t.getReceiverType().addMissingAnnotations(new HashSet<>(Arrays.asList(READONLY)));
                } else if (isMethodOrOverridingMethod(t, "equals(java.lang.Object)")) {
                    t.getReceiverType().addMissingAnnotations(new HashSet<>(Arrays.asList(READONLY)));
                    t.getParameterTypes().get(0).addMissingAnnotations(new HashSet<>(Arrays.asList(READONLY)));
                    // Don't specially handle clone(), just use standard defaulting way for instance methods
//                } else if (isMethodOrOverridingMethod(t, "clone()")) {
//                    t.getReceiverType().addMissingAnnotations(new HashSet<>(Arrays.asList(RECEIVERDEPENDANTMUTABLE)));
//                    t.getReturnType().addMissingAnnotations(new HashSet<>(Arrays.asList(RECEIVERDEPENDANTMUTABLE)));
//                }
                }
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
