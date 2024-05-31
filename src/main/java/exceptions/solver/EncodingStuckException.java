package exceptions.solver;

public class EncodingStuckException extends SolverException {
    public EncodingStuckException(String reason) {
        super(reason);
    }
}
