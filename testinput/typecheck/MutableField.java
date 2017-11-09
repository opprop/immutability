package typecheck;

import qual.Immutable;
import qual.Mutable;

public class MutableField {

    // Allow mutable field now
    @Mutable Object f;
    static @Mutable Object sf = new @Mutable Object();

    MutableField() {
        f = new @Mutable Object();
    }
}
