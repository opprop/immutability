package pico.inference;

import exceptions.UnkownImmutabilityQualifierException;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.FrameworkViewpointAdapter;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;

/**
 * This class exists because in inference side, we only support a reduced version of mutability qualifier
 * hierarchy.
 */
public class PICORealViewpointAdapter extends FrameworkViewpointAdapter {
    @Override
    protected AnnotationMirror combineModifierWithModifier(AnnotationMirror recvModifier, AnnotationMirror declModifier, AnnotatedTypeFactory f) {
        PICORealTypeFactory picoRealTypeFactory = (PICORealTypeFactory) f;
        if (AnnotationUtils.areSame(declModifier, picoRealTypeFactory.READONLY)) {
            return picoRealTypeFactory.READONLY;
        } else if (AnnotationUtils.areSame(declModifier, picoRealTypeFactory.MUTABLE)) {
            return picoRealTypeFactory.MUTABLE;
        } else if (AnnotationUtils.areSame(declModifier, picoRealTypeFactory.IMMUTABLE)) {
            return picoRealTypeFactory.IMMUTABLE;
        } else if (AnnotationUtils.areSame(declModifier, picoRealTypeFactory.BOTTOM)) {
            return picoRealTypeFactory.BOTTOM;
        } else if (AnnotationUtils.areSame(declModifier, picoRealTypeFactory.RECEIVERDEPENDANTMUTABLE)) {
            return recvModifier;
        } else {
            ErrorReporter.errorAbort("Unkown declared modifier: " + declModifier, new UnkownImmutabilityQualifierException());
            return null; // Unreachable code. Just to make compiler happy.
        }
    }

    @Override
    protected AnnotationMirror getModifier(AnnotatedTypeMirror atm, AnnotatedTypeFactory f) {
        return atm.getAnnotationInHierarchy(((PICORealTypeFactory)f).READONLY);
    }

    @Override
    public boolean shouldBeAdapted(AnnotatedTypeMirror type, Element element) {
        if (type.getKind() != TypeKind.DECLARED && type.getKind() != TypeKind.ARRAY) {
            return false;
        }
        return true;
    }
}
