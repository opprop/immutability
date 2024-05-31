import qual.Immutable;
import qual.Mutable;
import qual.ReceiverDependentMutable;

class InitializationBlockProblem {
    @ReceiverDependentMutable Object o;

    {
        // :: fixable-error: (assignment.type.incompatible)
        this.o = new @Immutable Object();
    }
}
