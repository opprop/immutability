package exceptions.solver;

/** Created by mier on 04/11/17. */
public class EncodingStuckException extends SolverException {
    public EncodingStuckException(String reason) {
        super(reason);
    }
}
