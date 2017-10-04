package pico.typecheck;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewClassTree;
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
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
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
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
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
        typeAnnotators.add(new PICOImplicitsTypeAnnotator(this));
        typeAnnotators.add(new CommitmentTypeAnnotator(this));
        typeAnnotators.add(new PICOImplicitsTypeAnnotator(this));
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
    public Pair<AnnotatedExecutableType, java.util.List<AnnotatedTypeMirror>> constructorFromUse(NewClassTree tree) {
        Pair<AnnotatedExecutableType, java.util.List<AnnotatedTypeMirror>> mfuPair =
                super.constructorFromUse(tree);
        AnnotatedExecutableType method = mfuPair.first;
        if (dependentTypesHelper != null) {
            dependentTypesHelper.viewpointAdaptConstructor(tree, method);
        }
        return mfuPair;
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

    @Override
    protected boolean hasFieldInvariantAnnotation(AnnotatedTypeMirror type) {
        // This affects which fields should be guaranteed to be initialized
        Set<AnnotationMirror> lowerBounds =
                AnnotatedTypes.findEffectiveLowerBoundAnnotations(qualHierarchy, type);
        return AnnotationUtils.containsSame(lowerBounds, IMMUTABLE) || AnnotationUtils.containsSame(lowerBounds, RECEIVERDEPENDANTMUTABLE);
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
        public boolean isSubtype(AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype) {
            if (subtype.getUnderlyingType().getKind().isPrimitive() ||
                    supertype.getUnderlyingType().getKind().isPrimitive() ) {
                // Ignore boxing/unboxing, mutability and initialization qualifiers for primitive types and boxed types
                return true;
            }
            // Below is not correct. Otherwise, if @Immutable Object is upper bound of T, @Mutable A can also be passed in
 /*           if (TypesUtils.isObject(subtype.getUnderlyingType()) || TypesUtils.isObject(supertype.getUnderlyingType())) {
                // Object doesn't have any fields. So it's not possible to mutate any fields of subclass
                // and break its immutability contracts. Assigning to or from Object is freely allowed.
                return true;
            }*/
            return super.isSubtype(subtype, supertype);
        }

        @Override
        public boolean isSubtype(AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype, AnnotationMirror top) {
            if (AnnotationUtils.areSame(top, READONLY)) {
                if (TypesUtils.isString(supertype.getUnderlyingType())) {
                    return true;
                }
            }
            return super.isSubtype(subtype, supertype, top);
        }
    }

    private void addDefaultIfStaticField(AnnotatedTypeMirror annotatedTypeMirror, Element element) {
        if (element != null && element.getKind() == ElementKind.FIELD && ElementUtils.isStatic(element)) {
            AnnotatedTypeMirror explicitATM = fromElement(element);
            if (!explicitATM.isAnnotatedInHierarchy(READONLY)) {
                if (PICOTypeUtil.isPrimitiveBoxedPrimitiveOrString(explicitATM)) {
                    annotatedTypeMirror.replaceAnnotation(IMMUTABLE);
                } else {
                    annotatedTypeMirror.replaceAnnotation(MUTABLE);
                }
            }
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

        /*This is copied because it has private access in super class. TODO should remove this duplicate code*/
        private boolean hasPrimaryAnnotationInAllHierarchies(AnnotatedTypeMirror type) {
            boolean annotated = true;
            for (AnnotationMirror top : qualHierarchy.getTopAnnotations()) {
                if (type.getEffectiveAnnotationInHierarchy(top) == null) {
                    annotated = false;
                }
            }
            return annotated;
        }

        // TODO Override all methods that calls addMissingAnnotations():
        // if the target type is primitive, boxed primitive or string,
        // override the type to be @Immutable

        @Override
        public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
            super.visitBinary(node, type);
            if (PICOTypeUtil.isPrimitiveBoxedPrimitiveOrString(type)) {
                type.replaceAnnotation(IMMUTABLE);
            }
            return null;
        }

        @Override
        public Void visitTypeCast(TypeCastTree node, AnnotatedTypeMirror type) {
            if (hasPrimaryAnnotationInAllHierarchies(type)) {
                // If the type is already has a primary annotation in all hierarchies, then the
                // propagated annotations won't be applied.  So don't compute them.
                return null;
            }

            AnnotatedTypeMirror exprType = atypeFactory.getAnnotatedType(node.getExpression());
            if (type.getKind() == TypeKind.TYPEVAR) {
                if (exprType.getKind() == TypeKind.TYPEVAR) {
                    // If both types are type variables, take the direct annotations.
                    type.addMissingAnnotations(exprType.getAnnotations());
                }
                // else do nothing.
            } else {
                // Use effective annotations from the expression, to get upper bound
                // of type variables.
                type.addMissingAnnotations(exprType.getEffectiveAnnotations());
                /*Difference Starts*/
                // If the type is primitive, boxed primitive or string, should replace the annotation to immutable
                if (PICOTypeUtil.isPrimitiveBoxedPrimitiveOrString(type)) {
                    type.replaceAnnotation(IMMUTABLE);
                }
                /*Difference Ends*/
            }
            return null;
        }
    }

    @Override
    protected void applyInferredAnnotations(AnnotatedTypeMirror type, PICOValue as) {
        super.applyInferredAnnotations(type, as);
        if (PICOTypeUtil.isBoxedPrimitiveOrString(type)) {
            type.replaceAnnotation(IMMUTABLE);
        }
    }
}
