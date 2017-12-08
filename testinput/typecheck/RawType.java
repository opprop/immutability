import java.util.ArrayList;
import java.util.Iterator;

interface A {

    public void foo();

}

public class RawType {

    private ArrayList list;

    protected void foo() {
        for (Iterator i = list.iterator(); i.hasNext();) {
            // Iterator is raw type here. It is represented in CF as Iterator<? extends @Readonly Object>.
            // E next() method returns ? extends @Readonly Object, which becomes @Readonly Object(Make sense?).
            // And then PICOPropagationTreeAnnotator propagates @Readonly annotation to A and type of (A) i.next()
            // in a whole becomes @Readonly.
            // :: error: (method.invocation.invalid)
            ((A) i.next()).foo();
        }
    }
}
