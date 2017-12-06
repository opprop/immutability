import qual.Immutable;
import qual.Mutable;
import qual.Readonly;

class ShouldFailButDidnot {
    void foo() {
        @Readonly Object o = new @Immutable Object();
        o = new @Mutable Object();
    }
}
