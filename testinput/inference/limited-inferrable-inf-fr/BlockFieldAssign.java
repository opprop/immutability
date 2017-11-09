public class BlockFieldAssign {
    Object o;

    {
        // TODO Fix "this" inside block has not variable annotation
        this.o = new Object();
    }
}
