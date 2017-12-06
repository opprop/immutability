package typecheck;

import qual.Immutable;
import qual.Mutable;
import qual.PolyMutable;
import qual.ReceiverDependantMutable;
import qual.Readonly;

@Immutable
class ImmutableClass1{
    // :: error: (constructor.return.incompatible) :: error: (type.invalid)
    @Mutable ImmutableClass1(Object o) {}
    // :: error: (constructor.return.incompatible)
    @ReceiverDependantMutable ImmutableClass1() {}
    @Immutable ImmutableClass1(@Immutable Number n) {}

    void method1(@Readonly ImmutableClass1 this) {}

    void method2(@Immutable ImmutableClass1 this) {}

    void method3(@ReceiverDependantMutable ImmutableClass1 this) {}

    void method4(@PolyMutable ImmutableClass1 this) {}

    // :: error: (method.receiver.incompatible) :: error: (type.invalid)
    void method5(@Mutable ImmutableClass1 this) {}

    // :: error: (method.receiver.incompatible)
    void method6(ImmutableClass1 this) {}
}
