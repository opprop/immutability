import qual.Immutable;
import qual.Mutable;
import qual.Readonly;

class A<T extends @Readonly Object>{
    T t;
    @Immutable A(T t){
        this.t = t;
    }
}

@Immutable
class ImmutableClass3 extends A<@Mutable Object>{
    @Immutable ImmutableClass3() {
        super(new @Mutable Object());
    }

    void foo(@Immutable ImmutableClass3 this) {
        /*This is acceptable. t is not in the abstract state of
        the entire object because T has upper bound @Readonly*/
        @Mutable Object mo = this.t;
    }
}
