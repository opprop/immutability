package pico.inference;

import checkers.inference.model.Slot;
import checkers.inference.util.InferenceViewpointAdapter;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import pico.typecheck.PICOTypeUtil;

import javax.lang.model.element.AnnotationMirror;

import static pico.typecheck.PICOAnnotationMirrorHolder.RECEIVER_DEPENDANT_MUTABLE;

public class PICOInferenceViewpointAdapter extends InferenceViewpointAdapter{

    @Override
    protected AnnotatedTypeMirror combineModifierWithType(Slot recvModifier, AnnotatedTypeMirror decl, AnnotatedTypeFactory f) {
        if (PICOTypeUtil.isImplicitlyImmutableType(decl)) {
            return decl;
        }
        return super.combineModifierWithType(recvModifier, decl, f);
    }
}
