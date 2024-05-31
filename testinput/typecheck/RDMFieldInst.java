import qual.*;

public class RDMFieldInst{
    @Mutable
    private static class MutableBox {}

    @Immutable
    private static class ImmutableBox {}

    @ReceiverDependentMutable
    private static class RDMBox {}

    @Immutable
    private static class ImmutableClass {
        // :: error: (type.invalid.annotations.on.use)
        @ReceiverDependentMutable MutableBox mutableBoxInRDM;
    }

    @Mutable
    private static class MutableClass {
        @ReceiverDependentMutable MutableBox mutableBoxInRDM = new MutableBox();

        @ReceiverDependentMutable RDMBox rdmBoxInRDMnewM = new @Mutable RDMBox();
        // :: error: (assignment.type.incompatible)
        @ReceiverDependentMutable RDMBox rdmBoxInRDMnewI = new @Immutable RDMBox();
        // :: error: (assignment.type.incompatible)
        @ReceiverDependentMutable RDMBox rdmBoxInRDMnewRDM = new @ReceiverDependentMutable RDMBox();
        // :: error: (type.invalid.annotations.on.use)
        @ReceiverDependentMutable ImmutableBox immutableBoxInRDM = new ImmutableBox();
    }



}
