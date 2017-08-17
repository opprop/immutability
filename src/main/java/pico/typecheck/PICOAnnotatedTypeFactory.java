package pico.typecheck;

import com.sun.tools.javac.util.List;
import org.checkerframework.checker.initialization.InitializationAnnotatedTypeFactory;
import org.checkerframework.checker.initialization.qual.FBCBottom;
import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.framework.util.ViewpointAdaptor;
import org.checkerframework.javacutil.AnnotationUtils;
import qual.Bottom;

import qual.Immutable;

import qual.Mutable;
import qual.PolyImmutable;
import qual.Readonly;

import javax.lang.model.element.AnnotationMirror;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class PICOAnnotatedTypeFactory extends InitializationAnnotatedTypeFactory<PICOValue,
        PICOStore, PICOTransfer, PICOAnalysis> {

    public final AnnotationMirror READONLY, MUTABLE, POLYIMMUTABLE, IMMUTABLE, BOTTOM;

    public PICOAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker, true);
        READONLY = AnnotationUtils.fromClass(elements, Readonly.class);
        MUTABLE = AnnotationUtils.fromClass(elements, Mutable.class);
        POLYIMMUTABLE = AnnotationUtils.fromClass(elements, PolyImmutable.class);
        IMMUTABLE = AnnotationUtils.fromClass(elements, Immutable.class);
        BOTTOM = AnnotationUtils.fromClass(elements, Bottom.class);
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
                        PolyImmutable.class,
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
    public AnnotationMirror getFieldInvariantAnnotation() {
        return IMMUTABLE;
    }

    @Override
    protected boolean hasFieldInvariantAnnotation(AnnotatedTypeMirror type) {
        return true;
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new PICOQualifierHierarchy(factory, (Object[]) null);
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
}
