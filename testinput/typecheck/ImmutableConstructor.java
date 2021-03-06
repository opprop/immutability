package typecheck;

import qual.Mutable;
import qual.Immutable;
import qual.ReceiverDependantMutable;
import qual.Readonly;

@Immutable
public class ImmutableConstructor {

    @Readonly Object rof;
    @ReceiverDependantMutable Object pif;
    @Immutable Object imf;

    // :: error: (initialization.fields.uninitialized)
    @Immutable ImmutableConstructor(@Mutable Object mo, @ReceiverDependantMutable Object po, @Immutable Object io) {
    }

    // Even if the first argument is @ReceiverDependantMutable, aliased @Mutable object cannot be captured by pif,
    // because @Immutable constructor return type only allows @Immutable object to be captured after
    // viewpoint adaptation. So it's still safe to have @ReceiverDependantMutable arguemnt in immutable constructor
    @Immutable ImmutableConstructor(@ReceiverDependantMutable Object po, @Immutable Object io) {
        this.rof = po;
        this.rof = io;

        this.pif = io;
        // :: error: (assignment.type.incompatible)
        this.pif = po;

        this.imf = io;
        // :: error: (assignment.type.incompatible)
        this.imf = po;
    }

    void invokeConstructor(@Readonly ImmutableConstructor this, @Readonly Object ro, @Mutable Object mo,
                           @ReceiverDependantMutable Object po, @Immutable Object io) {
        new @Immutable ImmutableConstructor(io, io);

        // :: error: (type.invalid.annotations.on.use)
        new @Mutable ImmutableConstructor(mo, io);

        // This no longer is error now(?). Because instantiating @Immutable constructor
        // as @PolyImmutable(PolymorphicQualifier) automatically resolves @PolyImmutable
        // to @Immutable, which might be a good thing
        // :: error: (type.invalid.annotations.on.use)
        new @ReceiverDependantMutable ImmutableConstructor(po, io);

        // :: error: (constructor.invocation.invalid) :: error: (pico.new.invalid)
        new @Readonly ImmutableConstructor(ro, io);
    }
}
