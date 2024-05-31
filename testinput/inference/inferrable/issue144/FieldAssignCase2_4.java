import org.checkerframework.checker.initialization.qual.UnderInitialization;
import qual.Immutable;
import qual.ReceiverDependentMutable;

public class FieldAssignCase2_4 {
    @ReceiverDependentMutable Object o;
    FieldAssignCase2_4() {
        init();
    }

    void init(@UnderInitialization FieldAssignCase2_4 this) {
        // :: fixable-error: (assignment.type.incompatible)
        this.o = new @Immutable Object();
    }
}
