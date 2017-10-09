package pico.typecheck;

import com.sun.tools.javac.code.Symbol.VarSymbol;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TypesUtils;
import qual.Assignable;

import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
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

    public static boolean isReceiverDependantAssignable(Element variableElement) {
        if (!(variableElement instanceof VariableElement)) return false;
        return ((VarSymbol)variableElement).type.getAnnotationMirrors().isEmpty();
    }

    public static boolean isAssignable(Element variableElement) {
        if (!(variableElement instanceof VariableElement)) return false;
        return ((VarSymbol)variableElement).type.getAnnotation(Assignable.class) != null;
    }

    public static boolean isFinal(Element variableElement) {
        if (!(variableElement instanceof VariableElement)) return false;
        return ElementUtils.isFinal(variableElement);
    }
}
