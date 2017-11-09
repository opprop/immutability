import qual.Mutable;
import qual.ReceiverDependantMutable;

public class ConstructorInvocationUsingNew {

    @ReceiverDependantMutable ConstructorInvocationUsingNew() {}

    public static void main(String[] args) {
        // Handled by PICOInferenceVisito#checkConstructorInvocability
        ConstructorInvocationUsingNew c = new ConstructorInvocationUsingNew();
    }
}
