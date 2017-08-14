package pico.typecheck;

import exceptions.UnkownImmutabilityQualifierException;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.FrameworkViewpointAdaptor;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;

/**
 * Created by mier on 20/06/17.
 */
public class PICOViewpointAdaptor extends FrameworkViewpointAdaptor {
    @Override
    protected AnnotationMirror combineModifierWithModifier(AnnotationMirror recvModifier, AnnotationMirror declModifier, AnnotatedTypeFactory f) {
        PICOAnnotatedTypeFactory iatf = (PICOAnnotatedTypeFactory)f;
        if (AnnotationUtils.areSame(declModifier, iatf.READONLY)) {
            return iatf.READONLY;
        } else if (AnnotationUtils.areSame(declModifier, iatf.MUTABLE)) {
            return iatf.MUTABLE;
        } else if (AnnotationUtils.areSame(declModifier, iatf.IMMUTABLE)) {
            return iatf.IMMUTABLE;
        } else if (AnnotationUtils.areSame(declModifier, iatf.POLYIMMUTABLE)) {
            return recvModifier;
        } else {
            ErrorReporter.errorAbort("Unkown declared modifier: " + declModifier, new UnkownImmutabilityQualifierException());
            return null; // Unreachable code. Just to make compiler happy.
        }
    }

    @Override
    protected AnnotationMirror getModifier(AnnotatedTypeMirror atm, AnnotatedTypeFactory f) {
        return atm.getAnnotationInHierarchy(((PICOAnnotatedTypeFactory)f).READONLY);
    }

    @Override
    public boolean shouldBeAdapted(AnnotatedTypeMirror type, Element element) {
        if (type.getKind() != TypeKind.DECLARED && type.getKind() != TypeKind.ARRAY) {
            return false;
        }
        return true;
    }
}
