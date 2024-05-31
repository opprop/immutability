import qual.Immutable;
import qual.Mutable;
import qual.ReceiverDependentMutable;

// A (main modifier is here) [] []
public class TwoDimensionalArray {

    public double @ReceiverDependentMutable[][] test () {
        @Immutable
        double @ReceiverDependentMutable [] @Mutable [] C = new @Immutable double @ReceiverDependentMutable [0][0];
        for (@Immutable int i = 0; i < 0; i++) {
            for (@Immutable int j = 0; j < 0; j++) {
                // Array C's main modifier is @ReceiverDependentMutable, so mutating C is not allowed
                // :: error: (illegal.array.write)
                C[i] = new double[]{1.0};
                // But C[i] is double @Mutable [](mutable array of double elements), so mutating C[i] is ALLOWED
                C[i][j] = 1.0;
            }
        }
        return C;
    }
}
