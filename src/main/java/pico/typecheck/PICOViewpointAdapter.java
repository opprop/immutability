package pico.typecheck;

import exceptions.UnkownImmutabilityQualifierException;
import org.checkerframework.framework.type.AbstractViewpointAdapter;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;

import static pico.typecheck.PICOAnnotationMirrorHolder.BOTTOM;
import static pico.typecheck.PICOAnnotationMirrorHolder.IMMUTABLE;
import static pico.typecheck.PICOAnnotationMirrorHolder.MUTABLE;
import static pico.typecheck.PICOAnnotationMirrorHolder.POLY_MUTABLE;
import static pico.typecheck.PICOAnnotationMirrorHolder.READONLY;
import static pico.typecheck.PICOAnnotationMirrorHolder.RECEIVER_DEPENDANT_MUTABLE;
import static pico.typecheck.PICOAnnotationMirrorHolder.SUBSTITUTABLE_POLY_MUTABLE;

/**
 * Created by mier on 20/06/17.
 */
public class PICOViewpointAdapter extends AbstractViewpointAdapter {

    public PICOViewpointAdapter(AnnotatedTypeFactory atypeFactory) {
        super(atypeFactory);
    }

    @Override
    protected boolean shouldAdaptMember(AnnotatedTypeMirror type, Element element) {
        if (!(type.getKind() == TypeKind.DECLARED || type.getKind() == TypeKind.ARRAY)) {
            return false;
        }
        return super.shouldAdaptMember(type, element);
    }

    @Override
    protected AnnotationMirror extractAnnotationMirror(AnnotatedTypeMirror atm) {
        return atm.getAnnotationInHierarchy(READONLY);
    }

    @Override
    protected AnnotationMirror combineAnnotationWithAnnotation(AnnotationMirror receiverAnnotation, AnnotationMirror declaredAnnotation) {
        if (AnnotationUtils.areSame(declaredAnnotation, READONLY)) {
            return READONLY;
        } else if (AnnotationUtils.areSame(declaredAnnotation, MUTABLE)) {
            return MUTABLE;
        } else if (AnnotationUtils.areSame(declaredAnnotation, IMMUTABLE)) {
            return IMMUTABLE;
        } else if (AnnotationUtils.areSame(declaredAnnotation, BOTTOM)) {
            return BOTTOM;
        } else if (AnnotationUtils.areSame(declaredAnnotation, POLY_MUTABLE)) {
            return SUBSTITUTABLE_POLY_MUTABLE;
        } else if (AnnotationUtils.areSame(declaredAnnotation, RECEIVER_DEPENDANT_MUTABLE)) {
            return receiverAnnotation;
        } else {
            ErrorReporter.errorAbort("Unkown declared modifier: " + declaredAnnotation, new UnkownImmutabilityQualifierException());
            return null; // Unreachable code. Just to make compiler happy.
        }
    }
//
//    @Override
//    protected AnnotationMirror getModifier(AnnotatedTypeMirror atm, AnnotatedTypeFactory f) {
//        return atm.getAnnotationInHierarchy(READONLY);
//    }

//    @Override
//    protected <TypeFactory extends AnnotatedTypeFactory> AnnotationMirror extractModifier(AnnotatedTypeMirror atm, TypeFactory f) {
//        return null;
//    }
}
