public class CompoundExpression {
    void foo(int i) {
        // For compound expression, VariableAnnotator doesn't handle binary tree "i+1" because
        // "i+1" doesn't exist in the source code. But PropagationTreeAnnotator still gets "i+1"
        // input tree and modifies the atm after VariableAnntator passed a round of inserting
        // VarAnnot to the least upper bound. So "i+1" atm doesn't have any VarAnnot, and doesn't
        // have a corresponding CombVariableSlot. Since "i+1" doesn't exist in real source code,
        // it isn't stored in the VariableAnnotator#treeToVarAnnoPair. So in PICOPropagationTreeAnnotator,
        // we just add the VarAnnot and the real annotation(will be removed from inference framework)
        // and don't need to worry about if corresponding slot is stored in SlotManager. Since there
        // is no place to insert to "i++" tree, so there isn't need to have Slot for this "i+1" tree.
        i++;
    }
}
