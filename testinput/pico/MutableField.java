import qual.Immutable;
import qual.Mutable;

public class MutableField {

    //:: error: (instance.field.mutable.forbidden)
    @Mutable Object f;
    // We do allow static mutable field, because it doesn't belong
    // to the abstract state of any object
    static @Mutable Object sf = new @Mutable Object();

    MutableField() {
        f = new @Mutable Object();
    }
}