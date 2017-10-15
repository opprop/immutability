package pico.typecheck;

import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TypesUtils;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public class PICOTypeUtil {

    private static boolean isPrimitive(AnnotatedTypeMirror atm) {
        TypeMirror tm = atm.getUnderlyingType();
        return TypesUtils.isPrimitive(tm);
    }

    private static boolean isBoxedPrimitive(AnnotatedTypeMirror atm) {
        TypeMirror tm = atm.getUnderlyingType();
        return TypesUtils.isBoxedPrimitive(tm);
    }

    public static boolean isString(AnnotatedTypeMirror atm) {
        TypeMirror tm = atm.getUnderlyingType();
        return TypesUtils.isString(tm);
    }

    /**Remember to add all the class names that are in @ImplicitFor in the declaration of @Immutable*/
    private static boolean isOtherImmutableImplicitType(AnnotatedTypeMirror atm) {
        if (atm.getKind() != TypeKind.DECLARED) {
            return false;
        }
        TypeMirror tm = atm.getUnderlyingType();
        String qualifiedName = TypesUtils.getQualifiedName((DeclaredType) tm).toString();
        return (qualifiedName.equals("java.lang.Number") ||
        qualifiedName.equals("java.math.BigDecimal") || qualifiedName.equals("java.math.BigInteger"));
    }

    public static boolean isImplicitlyImmutableType(AnnotatedTypeMirror atm) {
        return isPrimitive(atm) || isBoxedPrimitive(atm) || isString(atm) || isOtherImmutableImplicitType(atm);
    }
}
