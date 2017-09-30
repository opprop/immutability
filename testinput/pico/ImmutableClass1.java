import qual.Immutable;
import qual.Mutable;
import qual.PolyMutable;
import qual.ReceiverDependantMutable;
import qual.Readonly;

@Immutable
class ImmutableClass1{
    //:: error: (immutable.class.constructor.invalid)
    @Mutable ImmutableClass1(Object o) {}
    //:: error: (immutable.class.constructor.invalid)
    @ReceiverDependantMutable ImmutableClass1() {}
    @Immutable ImmutableClass1(@Immutable Number n) {}

    void method1(@Readonly ImmutableClass1 this) {}

    void method2(@Immutable ImmutableClass1 this) {}

    //:: error: (immutable.class.method.receiver.invalid)
    void method3(@ReceiverDependantMutable ImmutableClass1 this) {}

    //:: error: (immutable.class.method.receiver.invalid)
    void method4(@PolyMutable ImmutableClass1 this) {}

    //:: error: (immutable.class.method.receiver.invalid)
    void method5(@Mutable ImmutableClass1 this) {}
}
