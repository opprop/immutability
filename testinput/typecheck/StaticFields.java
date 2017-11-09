package typecheck;

import java.util.Date;

public class StaticFields {
    static Date d;// Not implicitly immutable type - Handled by PICOTreeAnnotator on declaration
    static Integer i;// Implicitly immutable type - Handled by PICOImplicitsTypeAnnotator

    static {
        d = new Date();// Handled by addComputedTypeAnnotations(Element, AnnotatedTypeMirror) when used
        i = 2;// Handled by PICOImplicitsTypeAnnotator when used
    }
}
