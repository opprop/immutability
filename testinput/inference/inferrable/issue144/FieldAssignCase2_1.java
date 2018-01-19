import qual.Immutable;
import qual.ReceiverDependantMutable;

public class FieldAssignCase2_1 {
    @ReceiverDependantMutable Object o;
    FieldAssignCase2_1(@Immutable Object ao) {
        // :: fixable-error: (assignment.type.incompatible)
        o = ao;
    }
}
