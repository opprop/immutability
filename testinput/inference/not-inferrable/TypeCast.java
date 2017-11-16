import qual.Mutable;

// https://github.com/topnessman/immutability-example/blob/master/meeting/2017_11.9_Meeting.txt
public class TypeCast {
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
