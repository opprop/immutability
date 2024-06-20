package pico.inference;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import pico.common.ExtendedViewpointAdapter;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

public class PICOInferenceExtendedViewpointAdapter extends PICOInferenceViewpointAdapter implements ExtendedViewpointAdapter {

    public PICOInferenceExtendedViewpointAdapter(AnnotatedTypeFactory atypeFactory) {
        super(atypeFactory);
    }

    /**
     * (Extended behaviour) viewpoint adapt super clause to its class declaration. Class declaration acts like receiver.
     * @param classType class declaration itself
     * @param superEle element of extends / implements clause
     * @param superType type of extends / implements clause
     */
    public void viewpointAdaptSuperClause(AnnotatedTypeMirror.AnnotatedDeclaredType classType, Element superEle, AnnotatedTypeMirror.AnnotatedDeclaredType superType) {
//        AnnotatedTypeMirror adapted = combineTypeWithType(classType, superType);
        AnnotationMirror adapted = combineAnnotationWithAnnotation(extractAnnotationMirror(classType), extractAnnotationMirror(superType));
        superType.replaceAnnotation(adapted);

    }

    public AnnotatedTypeMirror rawCombineAnnotationWithType(AnnotationMirror anno, AnnotatedTypeMirror type) {
        return combineAnnotationWithType(anno, type);
    }

    @Override
    public AnnotationMirror rawCombineAnnotationWithAnnotation(AnnotationMirror anno, AnnotationMirror type) {
        return combineAnnotationWithAnnotation(anno, type);
    }
}
