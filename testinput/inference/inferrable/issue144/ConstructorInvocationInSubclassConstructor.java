import qual.Immutable;

// https://github.com/typetools/annotation-tools/issues/144
// TODO https://github.com/opprop/checker-framework-inference/issues/109
public class ConstructorInvocationInSubclassConstructor {
    Object f;

    // :: fixable-error: (type.invalid.annotations.on.use) :: fixable-error: (constructor.return.incompatible)
    @Immutable ConstructorInvocationInSubclassConstructor(Object f) {
        // :: fixable-error: (assignment.type.incompatible)
        this.f = f;
    }
}

class SubClass extends ConstructorInvocationInSubclassConstructor {
    SubClass(Object p) {
        // Handled by PICOInferenceVisito##checkMethodInvocability
        // :: fixable-error: (super.constructor.invocation.incompatible)
        super(p);
    }
}
