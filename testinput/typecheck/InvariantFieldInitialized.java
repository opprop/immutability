package typecheck;

import qual.Immutable;
import qual.Mutable;
import qual.Readonly;
import qual.ReceiverDependantMutable;

public class InvariantFieldInitialized {
    @Immutable Object o1;
    @ReceiverDependantMutable Object o2;
    @Mutable Object o3;
    @Readonly Object o4;

    @Immutable InvariantFieldInitialized() {
        o1 = new @Immutable Object();
        o2 = new @Immutable Object();
    }
}
