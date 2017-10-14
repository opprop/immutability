package pico.typecheck;

import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TypesUtils;
import javax.lang.model.type.TypeMirror;

public class PICOTypeUtil {
    public static boolean isPrimitive(AnnotatedTypeMirror atm) {
        TypeMirror tm = atm.getUnderlyingType();
        return TypesUtils.isPrimitive(tm);
    }

    public static boolean isBoxedPrimitive(AnnotatedTypeMirror atm) {
        TypeMirror tm = atm.getUnderlyingType();
        return TypesUtils.isBoxedPrimitive(tm);
    }

    public static boolean isString(AnnotatedTypeMirror atm) {
        TypeMirror tm = atm.getUnderlyingType();
        return TypesUtils.isString(tm);
    }

    public static boolean isImplicitlyImmutableType(AnnotatedTypeMirror atm) {
        return isPrimitive(atm) || isBoxedPrimitive(atm) || isString(atm);
    }
}
