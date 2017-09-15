import qual.Immutable;
import qual.Mutable;
import qual.PolyImmutable;
import qual.Readonly;

public class Static{
    //:: error: (static.field.poly.forbidden)
    static @PolyImmutable Object o = new @PolyImmutable Object();
    static @PolyImmutable Object readStaticPolyField() {
        return o;
    }

    // We still allow assigning a poly object to a non-poly declared typed static field
    // because the it won't get mutable alias. It is guarded by the declared field type
    static @Readonly Object o2 = new @PolyImmutable Object();
    static @PolyImmutable Object readStaticReadonlyField() {
        // @PolyImmutable object stored in o2 will not get mutable alias by calling readStaticReadonlyField()
        //:: error: (return.type.incompatible)
        return o2;
    }

    static @Readonly Object o3 = createPolyObject();
    static @PolyImmutable Object createPolyObject() {
        return new @PolyImmutable Object();
    }


    Object f;
    @PolyImmutable Static() {
        f = o;
    }

    void test1() {
        @Immutable Static ims = new @Immutable Static();
        @Immutable Object alias1 = ims.f;
        @Mutable Static ms = new @Mutable Static();
        @Mutable Object alias2 = ms.f;
        // Call mutating methods on alias2 and static field o has two runtime semantics
        // ....
    }

    void test2() {
        @Immutable Object alias1 = Static.readStaticPolyField();
        // static field now get mutable alias and immutable alias
        @Mutable Object alias2 = Static.readStaticPolyField();
    }
}
