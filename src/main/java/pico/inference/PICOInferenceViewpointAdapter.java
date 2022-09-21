package pico.inference;

import checkers.inference.InferenceMain;
import checkers.inference.util.InferenceViewpointAdapter;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import pico.common.ExtendedViewpointAdapter;
import pico.common.PICOTypeUtil;
import static pico.typecheck.PICOAnnotationMirrorHolder.READONLY;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;

public class PICOInferenceViewpointAdapter extends InferenceViewpointAdapter implements ExtendedViewpointAdapter {

    public PICOInferenceViewpointAdapter(AnnotatedTypeFactory atypeFactory) {
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
    protected AnnotatedTypeMirror combineAnnotationWithType(AnnotationMirror receiverAnnotation, AnnotatedTypeMirror declared) {
        if (PICOTypeUtil.isImplicitlyImmutableType(declared)) {
            return declared;
        }
        // workaround
        if (InferenceMain.isHackMode()) {
            if (extractAnnotationMirror(declared) == null) {
                return declared;
            }
        }

        return super.combineAnnotationWithType(receiverAnnotation, declared);
    }

    @Override
    public AnnotatedTypeMirror rawCombineAnnotationWithType(AnnotationMirror anno, AnnotatedTypeMirror type) {
        return combineAnnotationWithType(anno, type);
    }

    @Override
    public AnnotationMirror rawCombineAnnotationWithAnnotation(AnnotationMirror anno, AnnotationMirror type) {
        return rawCombineAnnotationWithAnnotation(anno, type);
    }

    @Override
    protected AnnotationMirror extractAnnotationMirror(AnnotatedTypeMirror atm) {
        // since the introduction of vp-is-valid rules, real am may be used?
        AnnotationMirror am = super.extractAnnotationMirror(atm);
        if (am == null) {  // if failed, try to get real am
            am =  atm.getAnnotationInHierarchy(READONLY);
        }
        return am;
    }
}
