package typecheck;

import qual.Mutable;
import qual.Immutable;
import qual.ReceiverDependentMutable;
import qual.Readonly;

@ReceiverDependentMutable
public class ReceiverDependentMutableConstructor {

    @Readonly Object rof;
    @ReceiverDependentMutable Object pif;
    @Immutable Object imf;

    // :: error: (initialization.fields.uninitialized)
    @ReceiverDependentMutable ReceiverDependentMutableConstructor(@Mutable Object mo, @ReceiverDependentMutable Object po, @Immutable Object io) {
    }

    @ReceiverDependentMutable ReceiverDependentMutableConstructor(@ReceiverDependentMutable Object po, @Immutable Object io) {
        this.rof = po;
        this.rof = io;

        this.pif = po;
        // :: error: (assignment.type.incompatible)
        this.pif = io;

        // :: error: (assignment.type.incompatible)
        this.imf = po;
        this.imf = io;
    }

    void invokeConstructor(@Readonly Object ro, @Mutable Object mo, @ReceiverDependentMutable Object po, @Immutable Object io) {
        new @Mutable ReceiverDependentMutableConstructor(mo, io);
        // :: error: (argument.type.incompatible)
        new @Mutable ReceiverDependentMutableConstructor(ro, io);
        // :: error: (argument.type.incompatible)
        new @Mutable ReceiverDependentMutableConstructor(po, io);
        // :: error: (argument.type.incompatible)
        new @Mutable ReceiverDependentMutableConstructor(io, io);

        new @ReceiverDependentMutable ReceiverDependentMutableConstructor(po, io);
        // :: error: (argument.type.incompatible)
        new @ReceiverDependentMutable ReceiverDependentMutableConstructor(ro, io);
        // :: error: (argument.type.incompatible)
        new @ReceiverDependentMutable ReceiverDependentMutableConstructor(mo, io);
        // :: error: (argument.type.incompatible)
        new @ReceiverDependentMutable ReceiverDependentMutableConstructor(io, io);

        new @Immutable ReceiverDependentMutableConstructor(io, io);
        // :: error: (argument.type.incompatible)
        new @Immutable ReceiverDependentMutableConstructor(ro, io);
        // :: error: (argument.type.incompatible)
        new @Immutable ReceiverDependentMutableConstructor(mo, io);
        // :: error: (argument.type.incompatible)
        new @Immutable ReceiverDependentMutableConstructor(po, io);

        // :: error: (pico.new.invalid)
        new @Readonly ReceiverDependentMutableConstructor(ro, io);
    }
}
