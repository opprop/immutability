import qual.Mutable;
import qual.Immutable;
import qual.PolyImmutable;
import qual.Readonly;

public class ViewpointAdaptationRules {

    @Readonly Object rof;
    @PolyImmutable Object pif;
    @Immutable Object imf;

    //:: error: (consturctor.invalid)
    @PolyImmutable ViewpointAdaptationRules(@Readonly Object rof, @PolyImmutable Object pif, @Immutable Object imf) {
        this.rof = rof;
        this.pif = pif;
        this.imf = imf;
    }

    void mutatableReceiver(@Mutable ViewpointAdaptationRules this,
                           @Mutable Object mo, @PolyImmutable Object po, @Immutable Object io, @Readonly Object ro) {
        this.rof = mo;
        this.pif = mo;
        //:: error: (assignment.type.incompatible)
        this.imf = mo;


        this.rof = po;
        //:: error: (assignment.type.incompatible)
        this.pif = po;
        //:: error: (assignment.type.incompatible)
        this.imf = po;

        this.rof = io;
        //:: error: (assignment.type.incompatible)
        this.pif = io;
        this.imf = io;

        this.rof = ro;
        //:: error: (assignment.type.incompatible)
        this.pif = ro;
        //:: error: (assignment.type.incompatible)
        this.imf = ro;
    }

    void polyImmutableReceiver(@PolyImmutable ViewpointAdaptationRules this,
                               @Mutable Object mo, @PolyImmutable Object po, @Immutable Object io, @Readonly Object ro) {

        //:: error: (illegal.field.write)
        this.rof = mo;
        //:: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.pif = mo;
        //:: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.imf = mo;

        //:: error: (illegal.field.write)
        this.rof = po;
        //:: error: (illegal.field.write)
        this.pif = po;
        //:: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.imf = po;

        //:: error: (illegal.field.write)
        this.rof = io;
        //:: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.pif = io;
        //:: error: (illegal.field.write)
        this.imf = io;

        //:: error: (illegal.field.write)
        this.rof = ro;
        //:: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.pif = ro;
        //:: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.imf = ro;
    }

    void ImmutableReceiver(@Immutable ViewpointAdaptationRules this,
                               @Mutable Object mo, @PolyImmutable Object po, @Immutable Object io, @Readonly Object ro) {
        //:: error: (illegal.field.write)
        this.rof = mo;
        //:: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.pif = mo;
        //:: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.imf = mo;

        //:: error: (illegal.field.write)
        this.rof = po;
        //:: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.pif = po;
        //:: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.imf = po;

        //:: error: (illegal.field.write)
        this.rof = io;
        //:: error: (illegal.field.write)
        this.pif = io;
        //:: error: (illegal.field.write)
        this.imf = io;

        //:: error: (illegal.field.write)
        this.rof = ro;
        //:: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.pif = ro;
        //:: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.imf = ro;
    }

    void ReadonlyReceiver(@Readonly ViewpointAdaptationRules this,
                           @Mutable Object mo, @PolyImmutable Object po, @Immutable Object io, @Readonly Object ro) {
        //:: error: (illegal.field.write)
        this.rof = mo;
        //:: error: (illegal.field.write)
        this.pif = mo;
        //:: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.imf = mo;

        //:: error: (illegal.field.write)
        this.rof = po;
        //:: error: (illegal.field.write)
        this.pif = po;
        //:: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.imf = po;

        //:: error: (illegal.field.write)
        this.rof = io;
        //:: error: (illegal.field.write)
        this.pif = io;
        //:: error: (illegal.field.write)
        this.imf = io;

        //:: error: (illegal.field.write)
        this.rof = ro;
        //:: error: (illegal.field.write)
        this.pif = ro;
        //:: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.imf = ro;
    }
}