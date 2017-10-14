package pico.typecheck;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
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
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeReplacer;
import org.checkerframework.framework.type.DefaultTypeHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.TypeHierarchy;
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
import org.checkerframework.framework.util.QualifierPolymorphism;
import org.checkerframework.framework.util.ViewpointAdaptor;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import qual.Assignable;
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
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PICOAnnotatedTypeFactory extends InitializationAnnotatedTypeFactory<PICOValue,
        PICOStore, PICOTransfer, PICOAnalysis> {

    public final AnnotationMirror READONLY, MUTABLE, POLYMUTABLE
    , RECEIVERDEPENDANTMUTABLE, SUBSTITUTABLEPOLYMUTABLE, IMMUTABLE, BOTTOM, COMMITED;

    public PICOAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker, true);
        READONLY = AnnotationUtils.fromClass(elements, Readonly.class);
        MUTABLE = AnnotationUtils.fromClass(elements, Mutable.class);
        POLYMUTABLE = AnnotationUtils.fromClass(elements, PolyMutable.class);
        RECEIVERDEPENDANTMUTABLE = AnnotationUtils.fromClass(elements, ReceiverDependantMutable.class);
        SUBSTITUTABLEPOLYMUTABLE = AnnotationUtils.fromClass(elements, SubstitutablePolyMutable.class);
        IMMUTABLE = AnnotationUtils.fromClass(elements, Immutable.class);
        BOTTOM = AnnotationUtils.fromClass(elements, Bottom.class);

        COMMITED = AnnotationUtils.fromClass(elements, Initialized.class);
        addAliasedAnnotation(org.jmlspecs.annotation.Readonly.class, READONLY);
        postInit();
    }

    @Override
    protected boolean isInitializationAnnotation(AnnotationMirror anno) {
        return super.isInitializationAnnotation(anno);
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
    protected ViewpointAdaptor<?> createViewpointAdaptor() {
        return new PICOViewpointAdaptor();
    }

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                new PICOPropagationTreeAnnotator(this),
                new ImplicitsTreeAnnotator(this),
                new CommitmentTreeAnnotator(this),
                new PICOTreeAnnotator(this));
    }

    // TODO remove duplicated code
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
    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> methodFromUse(ExpressionTree tree, ExecutableElement methodElt, AnnotatedTypeMirror receiverType) {
        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> pair = super.methodFromUse(tree, methodElt, receiverType);
        // We want to replace polymutable with substitutablepolymutable when we invoke static methods
        // TODO Remove this hacky way
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

    @Override
    public AnnotationMirror getFieldInvariantAnnotation() {
        return IMMUTABLE;
    }

    @Override
    protected QualifierPolymorphism createQualifierPolymorphism() {
        return new QualifierPolymorphism(processingEnv, this){
            @Override
            public void annotate(NewClassTree tree, AnnotatedExecutableType type) {
                return;
            }
        };
    }

    @Override
    public void addComputedTypeAnnotations(Element elt, AnnotatedTypeMirror type) {
        addDefaultIfStaticField(type, elt);
        super.addComputedTypeAnnotations(elt, type);
    }

    private void addDefaultIfStaticField(AnnotatedTypeMirror annotatedTypeMirror, Element element) {
        if (element != null && element.getKind() == ElementKind.FIELD && ElementUtils.isStatic(element)) {
            AnnotatedTypeMirror explicitATM = fromElement(element);
            if (!explicitATM.isAnnotatedInHierarchy(READONLY)) {
                if (PICOTypeUtil.isImplicitlyImmutableType(explicitATM)) {
                    annotatedTypeMirror.replaceAnnotation(IMMUTABLE);
                } else {
                    annotatedTypeMirror.replaceAnnotation(MUTABLE);
                }
            }
        }
    }

    // This method allows get lhs WITH flow sensitive refinement
    // TODO Too much duplicate code. Should refactor it
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
    protected boolean hasFieldInvariantAnnotation(AnnotatedTypeMirror type, VariableElement fieldElement) {
        // This affects which fields should be guaranteed to be initialized
        Set<AnnotationMirror> lowerBounds =
                AnnotatedTypes.findEffectiveLowerBoundAnnotations(qualHierarchy, type);
        return (AnnotationUtils.containsSame(lowerBounds, IMMUTABLE) || AnnotationUtils.containsSame(lowerBounds, RECEIVERDEPENDANTMUTABLE))
                && !isAssignableField(fieldElement);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new PICOQualifierHierarchy(factory, (Object[]) null);
    }

    @Override
    protected TypeHierarchy createTypeHierarchy() {
        return new PICOTypeHierarchy(checker, qualHierarchy);
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

    protected class PICOTypeHierarchy extends DefaultTypeHierarchy {

        public PICOTypeHierarchy(BaseTypeChecker checker, QualifierHierarchy qualifierHierarchy) {
            super(checker, qualifierHierarchy, checker.hasOption("ignoreRawTypeArguments"),
                    checker.hasOption("invariantArrays"));
        }

        @Override
        public boolean isSubtype(AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype, AnnotationMirror top) {
            // Remove errors in toString() method that returns a field
            if (AnnotationUtils.areSame(top, READONLY)) {
                if (PICOTypeUtil.isString(supertype)) {
                    return true;
                }
            }
            return super.isSubtype(subtype, supertype, top);
        }
    }

    class PICOImplicitsTypeAnnotator extends ImplicitsTypeAnnotator {

        public PICOImplicitsTypeAnnotator(AnnotatedTypeFactory typeFactory) {
            super(typeFactory);
        }

        @Override
        public Void visitExecutable(AnnotatedExecutableType t, Void p) {
            // Also scan the receiver
            scan(t.getReceiverType(), p);
            return super.visitExecutable(t, p);
        }
    }

    class PICOTypeAnnotator extends TypeAnnotator {

        public PICOTypeAnnotator(AnnotatedTypeFactory typeFactory) {
            super(typeFactory);
        }

        @Override
        public Void visitExecutable(AnnotatedExecutableType t, Void p) {
            super.visitExecutable(t, p);
            if (isMethod(t, "toString") || isMethod(t, "hashCode")) {
                t.getReceiverType().addMissingAnnotations(new HashSet<>(Arrays.asList(READONLY)));
            } else if (isMethod(t, "equals")) {
                t.getReceiverType().addMissingAnnotations(new HashSet<>(Arrays.asList(READONLY)));
                t.getParameterTypes().get(0).addMissingAnnotations(new HashSet<>(Arrays.asList(READONLY)));
            } else if (isMethod(t, "clone")) {
                t.getReceiverType().addMissingAnnotations(new HashSet<>(Arrays.asList(RECEIVERDEPENDANTMUTABLE)));
                t.getReturnType().addMissingAnnotations(new HashSet<>(Arrays.asList(RECEIVERDEPENDANTMUTABLE)));
            }
            return null;
        }

        private boolean isMethod(AnnotatedExecutableType methodType, String methodName) {
            return methodType.getElement().getSimpleName().contentEquals(methodName);
        }
    }

    class PICOTreeAnnotator extends TreeAnnotator {
        public PICOTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitVariable(VariableTree node, AnnotatedTypeMirror annotatedTypeMirror) {
            VariableElement element = TreeUtils.elementFromDeclaration(node);
            // if not primitive type, boxed primitive or string then
            addDefaultIfStaticField(annotatedTypeMirror, element);
            return super.visitVariable(node, annotatedTypeMirror);
        }
    }

    class PICOPropagationTreeAnnotator extends PropagationTreeAnnotator {
        public PICOPropagationTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        // TODO Override all methods that calls addMissingAnnotations():
        // if the target type is primitive, boxed primitive or string,
        // override the type to be @Immutable

        @Override
        public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
            super.visitBinary(node, type);
            if (PICOTypeUtil.isImplicitlyImmutableType(type)) {
                type.replaceAnnotation(IMMUTABLE);
            }
            return null;
        }

        @Override
        public Void visitTypeCast(TypeCastTree node, AnnotatedTypeMirror type) {
            super.visitTypeCast(node, type);
            if (PICOTypeUtil.isImplicitlyImmutableType(type)) {
                type.replaceAnnotation(IMMUTABLE);
            }
            return null;
        }
    }

    @Override
    protected void applyInferredAnnotations(AnnotatedTypeMirror type, PICOValue as) {
        super.applyInferredAnnotations(type, as);
        if (PICOTypeUtil.isImplicitlyImmutableType(type)) {
            // If the dataflow refines the type as something not immutable, then we replace it with
            // immutable, because no other immutability qualifiers are allowed on primitive, boxed primitive and
            // Strings
            type.replaceAnnotation(IMMUTABLE);
        }
    }

    @Override
    public boolean getShouldDefaultTypeVarLocals() {
        return false;
    }

    protected boolean isAssignableField(Element variableElement) {
        assert variableElement instanceof VariableElement;
        return getDeclAnnotation(variableElement, Assignable.class) != null;
    }

    protected boolean isFinalField(Element variableElement) {
        assert variableElement instanceof VariableElement;
        return ElementUtils.isFinal(variableElement);
    }

    protected boolean isReceiverDependantAssignable(Element variableElement) {
        assert variableElement instanceof VariableElement;
        return !isAssignableField(variableElement) && !isFinalField(variableElement);
    }
}
