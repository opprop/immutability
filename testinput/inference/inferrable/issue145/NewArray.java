import qual.Immutable;
import qual.Mutable;

public class NewArray {
    void foo() {
        // https://github.com/typetools/annotation-tools/issues/145
        // :: fixable-error: (assignment.type.incompatible)
        @Immutable Object a = new Integer[0];
        // TODO why does annotation on Object(component) propagates to Integer is done on purpose?
        // I overrode this in commit: https://github.com/topnessman/immutability/commit/897d10942a33ee9c7f6384b91f41ece2f3fc20e9
        // :: fixable-error: (assignment.type.incompatible)
        Object @Immutable [] b = new Integer[0];
    }
}
