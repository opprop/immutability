import qual.Immutable;
import qual.Mutable;

public class NewArray {
    void foo() {
        // https://github.com/typetools/annotation-tools/issues/145
        // :: fixable-error: (assignment.type.incompatible)
        @Immutable Object a = new Integer[0];
        // TODO why does annotation on Object(component) propagates to Integer is done on purpose?
        // TODO Right now, we get "assignment.type.incompatible" error instead of expected "type.invalid".
        // This is becasue PICOInferenceVisitor and PICOInferenceValidator are not implemented to enforce
        // type rules in typechecking mode. I need to implement this.
        // :: fixable-error: (type.invalid)
        Object @Immutable [] b = new Integer[0];
    }
}
