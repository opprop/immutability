import qual.Mutable;

public class TypeCast {
    void foo(Object o) {
        if (o instanceof String) {
            String s = (String) o;
        } else if (o instanceof @Mutable Object) {
            @Mutable Object m = (@Mutable Object) o;
        }
    }
}
