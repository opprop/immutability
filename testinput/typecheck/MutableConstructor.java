package typecheck;

import qual.Mutable;
import qual.Immutable;
import qual.ReceiverDependentMutable;
import qual.Readonly;

public class MutableConstructor {

    @Readonly Object rof;
    @ReceiverDependentMutable Object pif;
    @Immutable Object imf;

    @Mutable MutableConstructor(@Mutable Object mo, @ReceiverDependentMutable Object po, @Immutable Object io) {
        // "this" automatically has the same type as constructor return type.
        this.rof = mo;
        this.rof = po;
        this.rof = io;

        this.pif = mo;
        // :: error: (assignment.type.incompatible)
        this.pif = po;
        // :: error: (assignment.type.incompatible)
        this.pif = io;

        // :: error: (assignment.type.incompatible)
        this.imf = mo;
        // :: error: (assignment.type.incompatible)
        this.imf = po;
        this.imf = io;
    }

    void invokeConstructor(@Mutable Object mo, @ReceiverDependentMutable Object po, @Immutable Object io) {
        new @Mutable MutableConstructor(mo, mo, io);
        // :: error: (argument.type.incompatible)
        new @Mutable MutableConstructor(mo, po, io);
        // The same argument as the one in ImmutableConstructor
        // :: error: (constructor.invocation.invalid)
        new @ReceiverDependentMutable MutableConstructor(mo, po, io);
        // :: error: (constructor.invocation.invalid)
        new @Immutable MutableConstructor(mo, io, io);
    }
}
