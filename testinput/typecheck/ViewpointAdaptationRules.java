package typecheck;

import qual.Assignable;
import qual.Mutable;
import qual.Immutable;
import qual.Readonly;
import qual.ReceiverDependentMutable;

@ReceiverDependentMutable
public class ViewpointAdaptationRules {

    @Assignable @Readonly Object rof;
    @ReceiverDependentMutable Object rdmf;
    @Immutable Object imf;
    @Assignable @Mutable Object mf;

    @ReceiverDependentMutable ViewpointAdaptationRules(@Readonly Object rof, @ReceiverDependentMutable Object rdmf, @Immutable Object imf, @Mutable Object mf) {
        this.rof = rof;
        this.rdmf = rdmf;
        this.imf = imf;
        this.mf = mf;
    }

    void mutatableReceiver(@Mutable ViewpointAdaptationRules this,
                           @Mutable Object mo, @ReceiverDependentMutable Object po, @Immutable Object io, @Readonly Object ro) {
        this.rof = mo;
        this.rdmf = mo;
        // :: error: (assignment.type.incompatible)
        this.imf = mo;
        this.mf = mo;

        this.rof = po;
        // :: error: (assignment.type.incompatible)
        this.rdmf = po;
        // :: error: (assignment.type.incompatible)
        this.imf = po;
        // :: error: (assignment.type.incompatible)
        this.mf = po;

        this.rof = io;
        // :: error: (assignment.type.incompatible)
        this.rdmf = io;
        this.imf = io;
        // :: error: (assignment.type.incompatible)
        this.mf = io;

        this.rof = ro;
        // :: error: (assignment.type.incompatible)
        this.rdmf = ro;
        // :: error: (assignment.type.incompatible)
        this.imf = ro;
        // :: error: (assignment.type.incompatible)
        this.mf = ro;
    }

    void receiverDependentMutableReceiver(@ReceiverDependentMutable ViewpointAdaptationRules this,
                               @Mutable Object mo, @ReceiverDependentMutable Object po, @Immutable Object io, @Readonly Object ro) {

        this.rof = mo;
        // :: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.rdmf = mo;
        // :: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.imf = mo;
        this.mf = mf;

        this.rof = po;
        // :: error: (illegal.field.write)
        this.rdmf = po;
        // :: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.imf = po;
        // :: error: (assignment.type.incompatible)
        this.mf = po;

        this.rof = io;
        // :: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.rdmf = io;
        // :: error: (illegal.field.write)
        this.imf = io;
        // :: error: (assignment.type.incompatible)
        this.mf = io;


        this.rof = ro;
        // :: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.rdmf = ro;
        // :: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.imf = ro;
        // :: error: (assignment.type.incompatible)
        this.mf = ro;
    }

    void ImmutableReceiver(@Immutable ViewpointAdaptationRules this,
                               @Mutable Object mo, @ReceiverDependentMutable Object po, @Immutable Object io, @Readonly Object ro) {
        this.rof = mo;
        // :: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.rdmf = mo;
        // :: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.imf = mo;
        this.mf = mf;

        this.rof = po;
        // :: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.rdmf = po;
        // :: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.imf = po;
        // :: error: (assignment.type.incompatible)
        this.mf = po;

        this.rof = io;
        // :: error: (illegal.field.write)
        this.rdmf = io;
        // :: error: (illegal.field.write)
        this.imf = io;
        // :: error: (assignment.type.incompatible)
        this.mf = io;

        this.rof = ro;
        // :: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.rdmf = ro;
        // :: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.imf = ro;
        // :: error: (assignment.type.incompatible)
        this.mf = ro;
    }

    void ReadonlyReceiver(@Readonly ViewpointAdaptationRules this,
                           @Mutable Object mo, @ReceiverDependentMutable Object po, @Immutable Object io, @Readonly Object ro) {
        //this.rof = mo;
        // :: error: (illegal.field.write)
        this.rdmf = mo;
        // :: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.imf = mo;
        this.mf = mo;

        this.rof = po;
        // :: error: (illegal.field.write)
        this.rdmf = po;
        // :: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.imf = po;
        // :: error: (assignment.type.incompatible)
        this.mf = po;

        this.rof = io;
        // :: error: (illegal.field.write)
        this.rdmf = io;
        // :: error: (illegal.field.write)
        this.imf = io;
        // :: error: (assignment.type.incompatible)
        this.mf = io;

        this.rof = ro;
        // :: error: (illegal.field.write)
        this.rdmf = ro;
        // :: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.imf = ro;
        // :: error: (assignment.type.incompatible)
        this.mf = ro;
    }
}
