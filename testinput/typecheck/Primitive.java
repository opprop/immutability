package typecheck;

import qual.Readonly;
import qual.Immutable;
import qual.Mutable;
import qual.ReceiverDependentMutable;

@Immutable
public class Primitive {
    // In the abstract state
    int implicitImmutableInt;
    @Immutable int validInt;
    // If you want to exclude primitive(including boxed primitive) and String from
    // abstract state, use @Readonly to do this, but not @Mutable, because they can't
    // be mutated conceptually.
    // :: error: (type.invalid.annotations.on.use)
    @Readonly int implicitOverridenInt;
    // :: error: (type.invalid.annotations.on.use)
    @Mutable int invalidInt;
    // :: error: (type.invalid.annotations.on.use)
    @ReceiverDependentMutable int invalidInt2;

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
