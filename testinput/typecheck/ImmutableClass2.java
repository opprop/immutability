package typecheck;

import qual.Immutable;
import qual.Mutable;
import qual.ReceiverDependantMutable;
import qual.Readonly;

// :: error: (immutable.class.type.parameter.bound.invalid)
@Immutable class ImmutableClass2<T extends @Readonly Object>{
    @Immutable ImmutableClass2(){}
}

// :: error: (immutable.class.type.parameter.bound.invalid)
@Immutable class ImmutableClass3<T extends @ReceiverDependantMutable Object>{
    @Immutable ImmutableClass3(){}
}

// :: error: (immutable.class.type.parameter.bound.invalid)
@Immutable class ImmutableClass4<T extends @Mutable Object>{
    @Immutable ImmutableClass4(){}
}

// :: error: (immutable.class.type.parameter.bound.invalid) :: error: (immutable.class.type.parameter.bound.invalid)
@Immutable class ImmutableClass5<T extends @Mutable Object, S extends T>{
    @Immutable ImmutableClass5(){}
}

@Immutable
class ImmutableClass6<T extends @Immutable Object>{
    @Immutable ImmutableClass6(){}
}

@Immutable class ImmutableClass7{
    @Immutable ImmutableClass7(){}
    // Should NOT have warnings for type parameter with non-immutable upper bound
    // if the type parameter is declared on generic method(?)
    <S extends @Mutable Object> S foo(@Immutable ImmutableClass7 this) {
        return null;
    }
}
