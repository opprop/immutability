import qual.ReceiverDependantMutable;

public class FieldAssignCase2_2 {
    @ReceiverDependantMutable Object o;
    FieldAssignCase2_2(@ReceiverDependantMutable Object o) {
        // :: fixable-error: (assignment.type.incompatible)
        this.o = o;
    }
}
