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
public class PICOInferenceRealViewpointAdapter extends FrameworkViewpointAdapter {
    @Override
    protected AnnotationMirror combineModifierWithModifier(AnnotationMirror recvModifier, AnnotationMirror declModifier, AnnotatedTypeFactory f) {
        PICOInferenceRealTypeFactory picoInferenceRealTypeFactory = (PICOInferenceRealTypeFactory) f;
        if (AnnotationUtils.areSame(declModifier, picoInferenceRealTypeFactory.READONLY)) {
            return picoInferenceRealTypeFactory.READONLY;
        } else if (AnnotationUtils.areSame(declModifier, picoInferenceRealTypeFactory.MUTABLE)) {
            return picoInferenceRealTypeFactory.MUTABLE;
        } else if (AnnotationUtils.areSame(declModifier, picoInferenceRealTypeFactory.IMMUTABLE)) {
            return picoInferenceRealTypeFactory.IMMUTABLE;
        } else if (AnnotationUtils.areSame(declModifier, picoInferenceRealTypeFactory.BOTTOM)) {
            return picoInferenceRealTypeFactory.BOTTOM;
        } else if (AnnotationUtils.areSame(declModifier, picoInferenceRealTypeFactory.RECEIVERDEPENDANTMUTABLE)) {
            return recvModifier;
        } else {
            ErrorReporter.errorAbort("Unkown declared modifier: " + declModifier, new UnkownImmutabilityQualifierException());
            return null; // Unreachable code. Just to make compiler happy.
        }
    }

    @Override
    protected AnnotationMirror getModifier(AnnotatedTypeMirror atm, AnnotatedTypeFactory f) {
        return atm.getAnnotationInHierarchy(((PICOInferenceRealTypeFactory)f).READONLY);
    }
}
