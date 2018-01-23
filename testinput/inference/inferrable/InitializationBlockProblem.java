import qual.Immutable;
import qual.Mutable;
import qual.ReceiverDependantMutable;

class InitializationBlockProblem {
    @ReceiverDependantMutable Object o;

    {
        // :: fixable-error: (assignment.type.incompatible)
        this.o = new @Immutable Object();
    }
}
