import org.checkerframework.checker.initialization.qual.UnderInitialization;
import qual.Immutable;
import qual.ReceiverDependantMutable;

public class FieldAssignCase2_4 {
    @ReceiverDependantMutable Object o;
    FieldAssignCase2_4() {
        init();
    }

    void init(@UnderInitialization FieldAssignCase2_4 this) {
        // :: fixable-error: (assignment.type.incompatible)
        this.o = new @Immutable Object();
    }
}
