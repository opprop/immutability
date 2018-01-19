package pico.typecheck;

import com.sun.source.tree.VariableTree;
import org.checkerframework.checker.initialization.InitializationTransfer;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.NullLiteralNode;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

import javax.lang.model.element.VariableElement;

/**
 * Created by mier on 15/08/17.
 */
public class PICOTransfer extends InitializationTransfer<PICOValue, PICOTransfer, PICOStore>{

    public PICOTransfer(PICOAnalysis analysis) {
        super(analysis);
    }

    @Override
    public TransferResult<PICOValue, PICOStore> visitAssignment(AssignmentNode n, TransferInput<PICOValue, PICOStore> in) {
        if (n.getExpression() instanceof NullLiteralNode && n.getTarget().getTree() instanceof VariableTree) {
            VariableElement varElement = TreeUtils.elementFromDeclaration((VariableTree) n.getTarget().getTree());
            // Below is for removing false positive warning of bottom illegal write cacused by refining field to @Bottom if
            // field initializer is null.
            // Forbid refinement from null literal in initializer to fields variable tree(identifier tree not affected, e.g.
            // assigning a field as null in instance methods or constructors)
            if (varElement != null && varElement.getKind().isField()) {
                PICOStore store = in.getRegularStore();
                PICOValue storeValue = in.getValueOfSubNode(n);
                PICOValue factoryValue = getValueFromFactory(n.getTree(), n);
                PICOValue value = moreSpecificValue(factoryValue, storeValue);
                return new RegularTransferResult<>(finishValue(value, store), store);
            }
        }
        TransferResult<PICOValue, PICOStore> result = super.visitAssignment(n, in);
        return result;
    }
}
