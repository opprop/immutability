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
            // Conservative. Fine. Because we only know i.next() returns something that is bound by @Readonly Object.
            // So we can't gurantee that it's subtype of @Mutable hence we raise method.invocation.invalid error here.
            // :: error: (method.invocation.invalid)
            ((A) i.next()).foo();
        }
    }
}
