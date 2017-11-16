import org.checkerframework.checker.initialization.qual.UnderInitialization;
import qual.ReceiverDependantMutable;

public class FieldAssignCase2_3 {
    @ReceiverDependantMutable Object o;
    FieldAssignCase2_3() {
        init();
    }

    void init(@UnderInitialization FieldAssignCase2_3 this) {
        // :: fixable-error: (assignment.type.incompatible)
        o = new @ReceiverDependantMutable Object();
    }
}
