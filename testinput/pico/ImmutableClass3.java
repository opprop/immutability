import qual.Immutable;
import qual.Mutable;
import qual.PolyImmutable;
import qual.Readonly;

class A<T extends @Readonly Object>{
    T t;
    A(T t){
        this.t = t;
    }
}

@Immutable
class ImmutableClass3<T extends @Immutable Object> extends A<@Mutable Object>{
    @Immutable ImmutableClass3() {
        super(new @Mutable Object());
    }
}
