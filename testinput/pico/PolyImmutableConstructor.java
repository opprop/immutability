import qual.Mutable;
import qual.Immutable;
import qual.PolyImmutable;
import qual.Readonly;

public class PolyImmutableConstructor {

    @Readonly Object rof;
    @PolyImmutable Object pif;
    @Immutable Object imf;

    //:: error: (consturctor.invalid)
    @PolyImmutable PolyImmutableConstructor(@Mutable Object mo, @PolyImmutable Object po, @Immutable Object io) {
    }

    @PolyImmutable PolyImmutableConstructor(@PolyImmutable Object po, @Immutable Object io) {
        //:: error: (illegal.write)
        this.rof = po;
        //:: error: (illegal.write)
        this.rof = io;

        //:: error: (illegal.write)
        this.pif = po;
        //:: error: (assignment.type.incompatible) :: error: (illegal.write)
        this.pif = io;

        //:: error: (assignment.type.incompatible) :: error: (illegal.write)
        this.imf = po;
        //:: error: (illegal.write)
        this.imf = io;
    }

    void invokeConstructor(@Readonly Object ro, @Mutable Object mo, @PolyImmutable Object po, @Immutable Object io) {
        new @Mutable PolyImmutableConstructor(mo, io);
        //:: error: (argument.type.incompatible)
        new @Mutable PolyImmutableConstructor(ro, io);
        //:: error: (argument.type.incompatible)
        new @Mutable PolyImmutableConstructor(po, io);
        //:: error: (argument.type.incompatible)
        new @Mutable PolyImmutableConstructor(io, io);

        new @PolyImmutable PolyImmutableConstructor(po, io);
        //:: error: (argument.type.incompatible)
        new @PolyImmutable PolyImmutableConstructor(ro, io);
        //:: error: (argument.type.incompatible)
        new @PolyImmutable PolyImmutableConstructor(mo, io);
        //:: error: (argument.type.incompatible)
        new @PolyImmutable PolyImmutableConstructor(io, io);

        new @Immutable PolyImmutableConstructor(io, io);
        //:: error: (argument.type.incompatible)
        new @Immutable PolyImmutableConstructor(ro, io);
        //:: error: (argument.type.incompatible)
        new @Immutable PolyImmutableConstructor(mo, io);
        //:: error: (argument.type.incompatible)
        new @Immutable PolyImmutableConstructor(po, io);

        //:: error: (pico.new)
        new @Readonly PolyImmutableConstructor(ro, io);
    }
}