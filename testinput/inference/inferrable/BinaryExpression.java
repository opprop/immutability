public class BinaryExpression {
    void foo(int i) {
        // For explicit binary tree, VariableAnnotator uses handleBinaryTree to store the mapping
        // between the binary tree and the least upper bound VarAnnot. So tree "i+1" has VarAnnot(x)
        // in the atm
        i = i + 1;
    }
}
