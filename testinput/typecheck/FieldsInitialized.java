package typecheck;

import qual.Assignable;
import qual.Immutable;
import qual.Mutable;
import qual.Readonly;
import qual.ReceiverDependentMutable;

@ReceiverDependentMutable
public class FieldsInitialized {
    final @Immutable Object f1;
    @Immutable Object f2;
    final @ReceiverDependentMutable Object f3;
    @ReceiverDependentMutable Object f4;

    @Mutable Object f5;
    @Readonly Object f6;
    @Assignable @Immutable Object f7;
    @Assignable @ReceiverDependentMutable Object f8;
    @Assignable @Mutable Object f9;
    @Assignable @Readonly Object f10;
    // :: error: (initialization.fields.uninitialized)
    @ReceiverDependentMutable FieldsInitialized() {
        f1 = new @Immutable Object();
        f2 = new @Immutable Object();
        f3 = new @ReceiverDependentMutable Object();
        f4 = new @ReceiverDependentMutable Object();
        f5 = new @Mutable Object();
        f6 = new @Immutable Object();
    }

}
