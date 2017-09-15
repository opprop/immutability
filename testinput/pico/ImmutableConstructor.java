import qual.Mutable;
import qual.Immutable;
import qual.PolyImmutable;
import qual.Readonly;

public class ImmutableConstructor {

    @Readonly Object rof;
    @PolyImmutable Object pif;
    @Immutable Object imf;

    //:: error: (constructor.invalid) :: error: (initialization.fields.uninitialized)
    @Immutable ImmutableConstructor(@Mutable Object mo, @PolyImmutable Object po, @Immutable Object io) {
    }

    // Even if the first argument is @PolyImmutable, aliased @Mutable object cannot be captured by pif,
    // because @Immutable constructor return type only allows @Immutable object to be captured after
    // viewpoint adaptation. So it's still safe to have @PolyImmutable arguemnt in immutable constructor
    @Immutable ImmutableConstructor(@PolyImmutable Object po, @Immutable Object io) {
        this.rof = po;
        this.rof = io;

        this.pif = io;
        //:: error: (assignment.type.incompatible)
        this.pif = po;

        this.imf = io;
        //:: error: (assignment.type.incompatible)
        this.imf = po;
    }

    void invokeConstructor(@Readonly Object ro, @Mutable Object mo, @PolyImmutable Object po, @Immutable Object io) {
        new @Immutable ImmutableConstructor(io, io);

        //:: error: (constructor.invocation.invalid)
        new @Mutable ImmutableConstructor(mo, io);

        // This no longer is error now(?). Because instantiating @Immutable constructor
        // as @PolyImmutable(PolymorphicQualifier) automatically resolves @PolyImmutable
        // to @Immutable, which might be a good thing
        new @PolyImmutable ImmutableConstructor(po, io);

        //:: error: (constructor.invocation.invalid) :: error: (pico.new.invalid)
        new @Readonly ImmutableConstructor(ro, io);
    }
}