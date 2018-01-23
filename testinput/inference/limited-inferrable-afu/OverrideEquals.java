import qual.Mutable;
import qual.Readonly;

// Copied from typechecking side testcase. AFU bug causes @Immutable is not inserted
// back to class A's declaration position, but it is inferred as result - @Immutable.
// This example typecheckes before and after inference otherwise, which is a good example
// indicating flexible overriding as both @Mutable and @Immutable
class A {// bound annotation is not inserted here
    void foo(@Readonly Object o) {}
}
public class OverrideEquals extends A{
    @Override
    void foo(@Readonly Object o){}

    @Override
    public boolean equals(Object o) {
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
