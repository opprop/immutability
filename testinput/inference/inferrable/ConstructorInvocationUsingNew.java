import qual.Immutable;
import qual.Mutable;
import qual.ReceiverDependantMutable;

public class ConstructorInvocationUsingNew {

    @ReceiverDependantMutable ConstructorInvocationUsingNew() {}

    public static void main(String[] args) {
        // Handled by PICOInferenceVisito#checkConstructorInvocability
        // :: fixable-error: (assignment.type.incompatible)
        @Immutable ConstructorInvocationUsingNew c = new ConstructorInvocationUsingNew();
    }
}
