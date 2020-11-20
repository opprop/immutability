import qual.*;

public class RDMFieldInst{
    @Mutable
    private static class MutableBox {}

    @Immutable
    private static class ImmutableBox {}

    @ReceiverDependantMutable
    private static class RDMBox {}

    @Immutable
    // :: error: (initialization.fields.uninitialized)
    private static class ImmutableClass {
        // :: error: (test-key-1)
        @ReceiverDependantMutable MutableBox mutableBoxInRDM;
    }

    @Mutable
    private static class MutableClass {
        @ReceiverDependantMutable MutableBox mutableBoxInRDM = new MutableBox();

        @ReceiverDependantMutable RDMBox rdmBoxInRDMnewM = new @Mutable RDMBox();
        // :: error: (assignment.type.incompatible)
        @ReceiverDependantMutable RDMBox rdmBoxInRDMnewI = new @Immutable RDMBox();
        // :: error: (assignment.type.incompatible)
        @ReceiverDependantMutable RDMBox rdmBoxInRDMnewRDM = new @ReceiverDependantMutable RDMBox();
        // :: error: (assignment.type.incompatible)
        @ReceiverDependantMutable ImmutableBox immutableBoxInRDM = new ImmutableBox();
    }



}