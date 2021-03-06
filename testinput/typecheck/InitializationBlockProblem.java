package typecheck;

import qual.Immutable;
import qual.Mutable;
import qual.ReceiverDependantMutable;

class InitializationBlockProblem {
    @ReceiverDependantMutable Object o;

    {
        this.o = new @Mutable Object();
        // :: error: (assignment.type.incompatible)
        this.o = new @Immutable Object();
    }
}
