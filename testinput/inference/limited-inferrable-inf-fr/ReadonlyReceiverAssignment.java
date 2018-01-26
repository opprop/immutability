import qual.Readonly;

import java.util.Date;

// Inference doesn't use flow sensitive refinement for local variable node. So if d is @Readonly,
// there will be no solution. In typecheck side, however, this typechecks because d is refined to
// be @Mutable and allowed to call setTime() method.
class ReadonlyReceiverAssignment {

    void foo() {
        // See InferenceTransfer#visitAssignment() and CFAbstractTransfer#visitAssignment() for details
        @Readonly Date d = new Date();
        d.setTime(2);
    }
}
