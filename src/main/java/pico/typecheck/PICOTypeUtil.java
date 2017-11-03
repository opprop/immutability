package pico.typecheck;

import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TypesUtils;
import qual.Immutable;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;

public class PICOTypeUtil {

    private static boolean isInTypesOfImplicitForOfImmutable(AnnotatedTypeMirror atm) {
        ImplicitFor implicitFor = Immutable.class.getAnnotation(ImplicitFor.class);
        assert implicitFor != null;
        assert implicitFor.types() != null;
        for (TypeKind typeKind : implicitFor.types()) {
            if (typeKind == atm.getKind()) return true;
        }
        return false;
    }

    private static boolean isInTypeNamesOfImplicitForOfImmutable(AnnotatedTypeMirror atm) {
        if (atm.getKind() != TypeKind.DECLARED) {
            return false;
        }
        ImplicitFor implicitFor = Immutable.class.getAnnotation(ImplicitFor.class);
        assert implicitFor != null;
        assert implicitFor.typeNames() != null;
        Class<?>[] typeNames = implicitFor.typeNames();
        String fqn = TypesUtils.getQualifiedName((DeclaredType) atm.getUnderlyingType()).toString();
        for (int i = 0; i < typeNames.length; i++) {
            if (typeNames[i].getCanonicalName().toString().contentEquals(fqn)) return true;
        }
        return false;
    }

    /**Method to determine if the underlying type is implicitly immutable. This method is consistent
     * with the types and typeNames that are in @ImplicitFor in the definition of @Immutable qualifier*/
    public static boolean isImplicitlyImmutableType(AnnotatedTypeMirror atm) {
        return isInTypesOfImplicitForOfImmutable(atm) || isInTypeNamesOfImplicitForOfImmutable(atm);
    }
}
