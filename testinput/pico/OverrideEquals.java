import qual.Mutable;
import qual.Readonly;

/**This class is based on the assumption: Object class is implicitly @Readonly.
 * Otherwise, should skip test*/

class A {
    void foo(@Readonly Object o) {}
}
public class OverrideEquals extends A{
    @Override
    void foo(@Readonly Object o){}

    @Override
    //:: error: (override.param.invalid)
    public boolean equals(@Mutable Object o) {
        return super.equals(o);
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException exc) {
            throw new InternalError(); //should never happen since we are cloneable
        }
    }
}
