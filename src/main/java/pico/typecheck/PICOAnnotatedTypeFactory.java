package pico.typecheck;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by mier on 20/06/17.
 */
public class PICOAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    public AnnotationMirror READONLY, MUTABLE, POLYIMMUTABLE, IMMUTABLE, BOTTOM;

    public PICOAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        READONLY = AnnotationUtils.fromClass(elements, Readonly.class);
        MUTABLE = AnnotationUtils.fromClass(elements, Mutable.class);
        POLYIMMUTABLE = AnnotationUtils.fromClass(elements, PolyImmutable.class);
        IMMUTABLE = AnnotationUtils.fromClass(elements, Immutable.class);
        BOTTOM = AnnotationUtils.fromClass(elements, Bottom.class);
        postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        Set<Class<? extends Annotation>> annotations = new HashSet<>(Arrays.asList(Readonly.class, Mutable.class,
                PolyImmutable.class, Immutable.class, Bottom.class));
        return Collections.unmodifiableSet(annotations);
    }

    @Override
    protected ViewpointAdaptor<?> createViewpointAdaptor() {
        return new PICOViewpointAdaptor();
    }
}
