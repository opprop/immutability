package typecheck;

import qual.Readonly;
import qual.Immutable;
import qual.Mutable;
import qual.ReceiverDependantMutable;

public class Primitive {
    // In the abstract state
    int implicitImmutableInt;
    @Immutable int validInt;
    // If you want to exclude primitive(including boxed primitive) and String from
    // abstract state, use @Readonly to do this, but not @Mutable, because they can't
    // be mutated conceptually.
    // :: error: (type.invalid)
    @Readonly int implicitOverridenInt;
    // :: error: (type.invalid)
    @Mutable int invalidInt;
    // :: error: (type.invalid)
    @ReceiverDependantMutable int invalidInt2;

    // :: error: (initialization.fields.uninitialized)
    @Immutable Primitive() {
        // Allowed within constructor
        implicitImmutableInt = 0;
    }

    void mutateFields(@Immutable Primitive this) {
        // :: error: (illegal.field.write)
        implicitImmutableInt = 1;
    }
}
