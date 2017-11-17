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
            // TODO Does this make sense?
            PICOStore store = in.getRegularStore();
            PICOValue storeValue = in.getValueOfSubNode(n);
            PICOValue factoryValue = getValueFromFactory(n.getTree(), n);
            PICOValue value = moreSpecificValue(factoryValue, storeValue);
            // TODO Is this really forbidding null literal's types from refined into lhs?
            return new RegularTransferResult<>(finishValue(value, store), store);
        }
        return super.visitAssignment(n, in);
    }
}
