// https://github.com/topnessman/immutability-example/blob/master/meeting/2017_11.9_Meeting.txt
// TODO Really implement what's in the comment below
public class BlockFieldAssign {
    Object o;

    {
        // We can't insert variable annotation to type that has explicit trees.
        // So we assume "this" has implicit @Readonly type in initialization blocks conservatively,
        // and create a ConstantSlot and generates constraints based on this assumption. Then we
        // can see by cases studies how this approach is.
        this.o = new Object();
    }
}
