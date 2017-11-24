package pico.typecheck;

import org.checkerframework.checker.initialization.InitializationTransfer;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.NullLiteralNode;

/**
 * Created by mier on 15/08/17.
 */
public class PICOTransfer extends InitializationTransfer<PICOValue, PICOTransfer, PICOStore>{

    public PICOTransfer(PICOAnalysis analysis) {
        super(analysis);
    }

    @Override
    public TransferResult<PICOValue, PICOStore> visitAssignment(AssignmentNode n, TransferInput<PICOValue, PICOStore> in) {
        if (n.getExpression() instanceof NullLiteralNode) {
            // Forbid refinement from null literal
            PICOStore store = in.getRegularStore();
            PICOValue storeValue = in.getValueOfSubNode(n);
            PICOValue factoryValue = getValueFromFactory(n.getTree(), n);
            PICOValue value = moreSpecificValue(factoryValue, storeValue);
            return new RegularTransferResult<>(finishValue(value, store), store);
        }
        return super.visitAssignment(n, in);
    }
}
