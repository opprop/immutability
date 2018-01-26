import qual.Mutable;

// https://github.com/topnessman/immutability-example/blob/master/meeting/2017_11.9_Meeting.txt
public class TypeCast {
    // Allows flexible casting now: if only expression type is comparable to cast type, it would
    // ok in inference. This means we generate comparable constraint on type casting. BUT on
    // typechecking side, we still warns possible unsafe downcasting. This asymmetric rule makes
    // inference to be able to infer more projects, and for annotated programs, PICO still warns
    // about it so that programmers notice the downcasting potential risk.
    void foo(Object o) {
        if (o instanceof String) {
            // :: warning: (cast.unsafe)
            String s = (String) o;
        } else if (o instanceof @Mutable Object) {
            // :: warning: (cast.unsafe)
            @Mutable Object m = (@Mutable Object) o;
        }
    }
}
