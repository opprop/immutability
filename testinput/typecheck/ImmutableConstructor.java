package typecheck;

import qual.Mutable;
import qual.Immutable;
import qual.ReceiverDependentMutable;
import qual.Readonly;

@Immutable
public class ImmutableConstructor {

    @Readonly Object rof;
    @ReceiverDependentMutable Object pif;
    @Immutable Object imf;

    // :: error: (initialization.fields.uninitialized)
    @Immutable ImmutableConstructor(@Mutable Object mo, @ReceiverDependentMutable Object po, @Immutable Object io) {
    }

    // Even if the first argument is @ReceiverDependentMutable, aliased @Mutable object cannot be captured by pif,
    // because @Immutable constructor return type only allows @Immutable object to be captured after
    // viewpoint adaptation. So it's still safe to have @ReceiverDependentMutable arguemnt in immutable constructor
    @Immutable ImmutableConstructor(@ReceiverDependentMutable Object po, @Immutable Object io) {
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
                           @ReceiverDependentMutable Object po, @Immutable Object io) {
        new @Immutable ImmutableConstructor(io, io);

        // :: error: (constructor.invocation.invalid)
        new @Mutable ImmutableConstructor(mo, io);

        // constructor.invocation.invalid propgates before annotation invalid messages and stops
        // :: error: (constructor.invocation.invalid)
        new @ReceiverDependentMutable ImmutableConstructor(po, io);

        // :: error: (constructor.invocation.invalid) :: error: (pico.new.invalid)
        new @Readonly ImmutableConstructor(ro, io);
    }
}
