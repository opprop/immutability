import qual.Immutable;

public class ConstructorInvocationInSubclassConstructor {
    @Immutable ConstructorInvocationInSubclassConstructor() {

    }
}

class SubClass extends ConstructorInvocationInSubclassConstructor {
    SubClass() {
        // Handled by PICOInferenceVisito##checkMethodInvocability
        super();
    }
}
