import java.util.Arrays;
import java.util.Comparator;
import qual.*;

public class StrangeReadonly {
    @SuppressWarnings("unchecked")
    static void foo() {
        Integer[] indices = new Integer[0];
        Arrays.sort(indices, new Comparator() {
            // Type parameters for Comparator are different in typechecking side and inference side.
            // On typechecking side, it is "T extends @Mutable Object"; On inference side, it's
            // "T extends @Readonly Object". This caused inference to generate subtype constraint
            // : typeof(o1) :> @Readonly(actually equality constraint in implementation). So it caused
            // o1 and o2 always got inferred to @Readonly. Is this discrepency OK?

            // Update: the previous issue is related to testcase - NoBoundTypeOfAnonymousClass.java.
            // After the fix for missing bound for anonymous class, viewpoint adaptation correctly
            // infers type argument "? extends Object" to type variable "T extends @Readonly Object";
            // And CF right now ignores subtype relationship check(constraint generation on inference
            // side) and always returns true, i.e. "? extends Object" <: VarAnnot(o1), so typeof(o1) :> @Readonly
            // wasn't generated and o1 is inferred to @Immutable(select any valid solution).

            // Lian: CF now could correctly default the upper bound. @Readonly added on parameters.
            public int compare(@Readonly Object o1, @Readonly Object o2) {
                // Before inference, @Mutable is casted to @Immutable; After inference, @Readonly is
                // casted to @Immutable.
                // :: fixable-warning: (cast.unsafe)
                int i = (Integer) o1;
                // :: fixable-warning: (cast.unsafe)
                int j = (Integer) o2;
                return 0;
            }
        });
    }
}
