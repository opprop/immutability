import qual.Immutable;
import qual.ReceiverDependentMutable;

public class FieldAssignCase2_1 {
    @ReceiverDependentMutable Object o;
    FieldAssignCase2_1(@Immutable Object ao) {
        // :: fixable-error: (assignment.type.incompatible)
        o = ao;
    }
}
