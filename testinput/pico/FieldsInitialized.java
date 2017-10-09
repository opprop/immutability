import qual.Assignable;
import qual.Immutable;
import qual.Mutable;
import qual.Readonly;
import qual.ReceiverDependantMutable;

public class FieldsInitialized {
    final @Immutable Object f1;
    @Immutable Object f2;
    final @ReceiverDependantMutable Object f3;
    @ReceiverDependantMutable Object f4;

    @Mutable Object f5;
    @Readonly Object f6;
    @Assignable @Immutable Object f7;
    @Assignable @ReceiverDependantMutable Object f8;
    @Assignable @Mutable Object f9;
    @Assignable @Readonly Object f10;

    @ReceiverDependantMutable FieldsInitialized() {
        f1 = new @Immutable Object();
        f2 = new @Immutable Object();
        f3 = new @ReceiverDependantMutable Object();
        f4 = new @ReceiverDependantMutable Object();
    }

}
