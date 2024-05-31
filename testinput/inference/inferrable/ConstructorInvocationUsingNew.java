import qual.Immutable;
import qual.Mutable;
import qual.ReceiverDependentMutable;

public class ConstructorInvocationUsingNew {

    // :: fixable-error: (type.invalid.annotations.on.use)
    @ReceiverDependentMutable ConstructorInvocationUsingNew() {}

    public static void main(String[] args) {
        // Handled by PICOInferenceVisito#checkConstructorInvocability
        // :: fixable-error: (type.invalid.annotations.on.use)  :: fixable-error: (assignment.type.incompatible)
        @Immutable ConstructorInvocationUsingNew c = new ConstructorInvocationUsingNew();
    }
}
