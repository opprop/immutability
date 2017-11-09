package typecheck;

import qual.Immutable;
import qual.Readonly;
import qual.Mutable;
import qual.ReceiverDependantMutable;

public class ReadonlyMayCaptureMutable {
    static @Mutable Object smf = new @Mutable Object();

    @Readonly Object rof;
    @ReceiverDependantMutable ReadonlyMayCaptureMutable() {
        // Not a problem anymore, because readonly field is out of the abstract state
        rof = smf;
    }

    @Immutable ReadonlyMayCaptureMutable(@Immutable Object o) {
        // The same argument applies as above
        rof = smf;
    }

    @Mutable ReadonlyMayCaptureMutable(Object o1, Object o2) {
        rof = smf;
    }
}
