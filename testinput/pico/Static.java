import qual.Immutable;
import qual.Mutable;
import qual.PolyMutable;
import qual.Readonly;
import qual.ReceiverDependantMutable;

public class Static{
    //:: error: (static.receiverdependantmutable.forbidden)
    static @ReceiverDependantMutable Object o = new @ReceiverDependantMutable Object();
    static Object oo;

    Object f;
    @ReceiverDependantMutable Static() {
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

    //:: error: (static.receiverdependantmutable.forbidden)
    static @ReceiverDependantMutable Object readStaticReceiverDependantMutableField(@ReceiverDependantMutable Object p) {
        //:: error: (static.receiverdependantmutable.forbidden)
        return o;
        // TODO Avoid warnings for receiverdependantmutable fields in anonymous class
    }

    static {
        oo = new @Mutable Object();
        //:: error: (static.receiverdependantmutable.forbidden)
        @Readonly Object ro = (@ReceiverDependantMutable Object) o;
        //:: error: (static.receiverdependantmutable.forbidden)
        new @ReceiverDependantMutable Object();
    }

    //:: error: (static.receiverdependantmutable.forbidden)
    static @Readonly Object o2 = new @ReceiverDependantMutable Object();

    static @PolyMutable Object createPolyObject(@Immutable Object p) {
        return new @PolyMutable Object();
    }
    // TODO Hackily implemented. Should better implement it
    static @Mutable Object o3 = createPolyObject(new @Immutable Object());
}
