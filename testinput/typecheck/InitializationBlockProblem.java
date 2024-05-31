package typecheck;

import qual.Immutable;
import qual.Mutable;
import qual.ReceiverDependentMutable;

class InitializationBlockProblem {
    @ReceiverDependentMutable Object o;

    {
        this.o = new @Mutable Object();
        // :: error: (assignment.type.incompatible)
        this.o = new @Immutable Object();
    }
}
