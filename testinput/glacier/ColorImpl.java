// @skip-test
// Typecheck, Inference-TC: Failed
// Cause: checker cannot get clone() decl from stub! Decl receiver and return type get defaulted to mutable.
// strange enough, seems other signatures are got from stubs.
import java.util.Arrays;
import qual.Immutable;

public class ColorImpl {
    private byte @Immutable [] _rgb;
    private byte [] _mutableRgb;

    public int hashCode() {
        // This should be OK, but will be an error until we have a @ReadOnly annotation.
        Arrays.hashCode(_mutableRgb);

        return Arrays.hashCode(_rgb);
    }
}