package pico.typecheck;

import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractValue;

/** Created by mier on 15/08/17. */
public class PICOValue extends CFAbstractValue<PICOValue> {
    public PICOValue(
            CFAbstractAnalysis<PICOValue, ?, ?> analysis,
            Set<AnnotationMirror> annotations,
            TypeMirror underlyingType) {
        super(analysis, annotations, underlyingType);
    }
}
