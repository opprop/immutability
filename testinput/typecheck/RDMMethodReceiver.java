package typecheck;

import qual.Mutable;
import qual.Immutable;
import qual.ReceiverDependantMutable;
import qual.Readonly;

public class RDMMethodReceiver {

    public class ImmutableClassMember {
        @ReceiverDependantMutable Object get(@Immutable ImmutableClassMember this) { return null; }  
        Object set(@Immutable ImmutableClassMember this, @ReceiverDependantMutable Object x) { return null; }
    }

    public class MutableClassMember {
        @ReceiverDependantMutable Object get(@Mutable MutableClassMember this) { return null; }
        Object set(@Mutable MutableClassMember this, @ReceiverDependantMutable Object x) { return null; } 
    }

    void invoke() {
        ImmutableClassMember imc = new ImmutableClassMember();
        @Immutable Object x = imc.get();
        imc.set(x);

        MutableClassMember mc = new MutableClassMember();
        @Mutable Object y = mc.get();
        mc.set(y);

        // :: error: (assignment.type.incompatible) 
        @Immutable Object z = mc.get();
        // :: error: (argument.type.incompatible)
        mc.set(z);

        // :: error: (assignment.type.incompatible) 
        @Mutable Object w = imc.get();
        // :: error: (argument.type.incompatible)
        imc.set(w);
    }
}
