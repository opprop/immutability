package pico.typecheck;

import exceptions.UnkownImmutabilityQualifierException;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.FrameworkViewpointAdapter;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;

import javax.lang.model.element.AnnotationMirror;

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
public class PICOViewpointAdapter extends FrameworkViewpointAdapter {
    @Override
    protected AnnotationMirror combineModifierWithModifier(AnnotationMirror recvModifier, AnnotationMirror declModifier, AnnotatedTypeFactory f) {
        if (AnnotationUtils.areSame(declModifier, READONLY)) {
            return READONLY;
        } else if (AnnotationUtils.areSame(declModifier, MUTABLE)) {
            return MUTABLE;
        } else if (AnnotationUtils.areSame(declModifier, IMMUTABLE)) {
            return IMMUTABLE;
        } else if (AnnotationUtils.areSame(declModifier, BOTTOM)) {
            return BOTTOM;
        } else if (AnnotationUtils.areSame(declModifier, POLY_MUTABLE)) {
            return SUBSTITUTABLE_POLY_MUTABLE;
        } else if (AnnotationUtils.areSame(declModifier, RECEIVER_DEPENDANT_MUTABLE)) {
            return recvModifier;
        } else {
            ErrorReporter.errorAbort("Unkown declared modifier: " + declModifier, new UnkownImmutabilityQualifierException());
            return null; // Unreachable code. Just to make compiler happy.
        }
    }

    @Override
    protected AnnotationMirror getModifier(AnnotatedTypeMirror atm, AnnotatedTypeFactory f) {
        return atm.getAnnotationInHierarchy(READONLY);
    }
}
