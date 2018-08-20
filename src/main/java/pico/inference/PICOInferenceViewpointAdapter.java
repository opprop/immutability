package pico.inference;

import checkers.inference.util.InferenceViewpointAdapter;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import pico.typecheck.PICOTypeUtil;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;

public class PICOInferenceViewpointAdapter extends InferenceViewpointAdapter{

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
        return super.combineAnnotationWithType(receiverAnnotation, declared);
    }
}
