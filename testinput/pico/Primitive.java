import qual.Readonly;
import qual.Immutable;
import qual.Mutable;
import qual.ReceiverDependantMutable;

public class Primitive {
    // Implicitly in the abstract state
    int implicitImmutableInt;
    // If you want to exclude primitive(including boxed primitive) and String from
    // abstract state, use @Readonly to do this, but not @Mutable, because they can't
    // be mutated conceptually.
    @Readonly int implicitOverridenInt;
    //:: error: (type.invalid)
    @Mutable int invalidInt;
    //:: error: (type.invalid)
    @ReceiverDependantMutable int invalidInt2;

    //:: error: (initialization.fields.uninitialized)
    @Immutable Primitive() {
        // Allowed within constructor
        implicitImmutableInt = 0;
        implicitOverridenInt = 0;
    }

    void mutateFields(@Immutable Primitive this) {
        //:: error: (illegal.field.write)
        implicitImmutableInt = 1;
        implicitOverridenInt = 1;
    }
}