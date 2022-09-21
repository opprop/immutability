package typecheck;

import qual.Immutable;
import qual.Mutable;
import qual.PolyMutable;
import qual.ReceiverDependantMutable;
import qual.Readonly;

@Immutable
class ImmutableClass1{
    // :: error: (type.invalid.annotations.on.use)
    @Mutable ImmutableClass1(Object o) {}
    // :: error: (type.invalid.annotations.on.use)
    @ReceiverDependantMutable ImmutableClass1() {}
    @Immutable ImmutableClass1(@Immutable Number n) {}

    void method1(@Readonly ImmutableClass1 this) {}

    void method2(@Immutable ImmutableClass1 this) {}

    // :: error: (type.invalid.annotations.on.use) :: error: (method.receiver.incompatible)
    void method3(@ReceiverDependantMutable ImmutableClass1 this) {}

    void method4(@PolyMutable ImmutableClass1 this) {}

    // :: error: (method.receiver.incompatible) :: error: (type.invalid.annotations.on.use)
    void method5(@Mutable ImmutableClass1 this) {}


    // when not annotated explictly, default annotations of <this> is inherited from declaration
    void method6(ImmutableClass1 this) {}
}
