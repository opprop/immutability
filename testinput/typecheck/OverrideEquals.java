package typecheck;

import qual.Mutable;
import qual.Readonly;

class A {
    void foo(@Readonly Object o) {}
}
public class OverrideEquals extends A{
    @Override
    void foo(@Readonly Object o){}

    @Override
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

class SubOverrideEquals extends OverrideEquals {
    @Override
    public boolean equals(@Readonly Object o) {
        return super.equals(new @Mutable Object());
    }
}
