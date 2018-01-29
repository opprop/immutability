import qual.Mutable;

public class LUBNoSubtypeConForImmutableType {

    @Mutable Object o;

    void foo() {
        // Shouldn't create a LUBVariableSlot for least upper bound, nor should generate subtype
        // constraint between @Immutable "least upper bound" with o or string literal, because
        // implicitly immutable types are alreayd handle by PICOInferencePropagationTreeAnnotaot
        // so VariableAnnotator#handleBinaryTree() shouldn't handle it again.
        throw new IllegalArgumentException(o + " is not a positive long value");
    }
}
