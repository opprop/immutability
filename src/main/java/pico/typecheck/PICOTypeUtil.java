package pico.typecheck;

import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TypesUtils;
import javax.lang.model.type.TypeMirror;

public class PICOTypeUtil {
    public static boolean isPrimitive(AnnotatedTypeMirror atm) {
        TypeMirror tm = atm.getUnderlyingType();
        return TypesUtils.isPrimitive(tm);
    }

    public static boolean isBoxedPrimitiveOrString(AnnotatedTypeMirror atm) {
        TypeMirror tm = atm.getUnderlyingType();
        return TypesUtils.isString(tm) || TypesUtils.isBoxedPrimitive(tm);
    }

    public static boolean isPrimitiveBoxedPrimitiveOrString(AnnotatedTypeMirror atm) {
        return isPrimitive(atm) || isBoxedPrimitiveOrString(atm);
    }
}
