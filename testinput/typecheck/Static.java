package typecheck;

import qual.Immutable;
import qual.Mutable;
import qual.PolyMutable;
import qual.Readonly;
import qual.ReceiverDependentMutable;

@ReceiverDependentMutable
public class Static{
    // :: error: (static.receiverdependentmutable.forbidden)
    static @ReceiverDependentMutable Object o = new @ReceiverDependentMutable Object();
    static Object oo;

    @ReceiverDependentMutable Object f;
    @ReceiverDependentMutable Static() {
        f = o;
    }

    void twoRuntimeSemantics() {
        @Immutable Static ims = new @Immutable Static();
        @Immutable Object alias1 = ims.f;
        @Mutable Static ms = new @Mutable Static();
        @Mutable Object alias2 = ms.f;
        // Call mutating methods on alias2 and static field o has two runtime semantics
        // ....
    }

    // :: error: (static.receiverdependentmutable.forbidden)
    static @ReceiverDependentMutable Object readStaticReceiverDependentMutableField(@ReceiverDependentMutable Object p) {
        return o;
        // TODO Avoid warnings for receiverdependentmutable fields in anonymous class
    }

    static {
        oo = new @Mutable Object();
        // :: error: (static.receiverdependentmutable.forbidden)
        @Readonly Object ro = (@ReceiverDependentMutable Object) o;
        // :: error: (static.receiverdependentmutable.forbidden)
        new @ReceiverDependentMutable Object();
    }

    // :: error: (static.receiverdependentmutable.forbidden)
    static @Readonly Object o2 = new @ReceiverDependentMutable Object();

    static @PolyMutable Object createPolyObject(@Immutable Object p) {
        return new @PolyMutable Object();
    }
    // TODO Hackily implemented. Should better implement it
    static @Mutable Object o3 = createPolyObject(new @Immutable Object());
}
