public class FixableTypeCast {
    void foo(Object o) {
        // :: fixable-warning: (cast.unsafe)
        String s = (String) o;
    }
}
