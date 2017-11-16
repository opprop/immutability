import qual.Immutable;

// https://github.com/typetools/annotation-tools/issues/144
// TODO https://github.com/opprop/checker-framework-inference/issues/109
public class ConstructorInvocationInSubclassConstructor {
    @Immutable ConstructorInvocationInSubclassConstructor() {

    }
}

class SubClass extends ConstructorInvocationInSubclassConstructor {
    SubClass() {
        // Handled by PICOInferenceVisito##checkMethodInvocability
        // :: fixable-error: (constructor.invocation.invalid)
        super();
    }
}
