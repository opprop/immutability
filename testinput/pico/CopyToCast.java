import qual.Mutable;
import qual.Readonly;

import java.util.List;
import java.lang.CloneNotSupportedException;

public class CopyToCast {
    void foo(Object o) {
        String s = (String) o;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        // TODO Copy method receiver's annotation to super
        // TODO Defaults for four Object methods are fixed
        CopyToCast oe = (CopyToCast) super.clone();
        return oe;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
