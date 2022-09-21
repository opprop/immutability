import qual.*;

public class DeepMutable {
    @Mutable
    static class MutableBox {}

    @Immutable
    static class ImmutableClass {

        // :: warning: (implicit.shallow.immutable)
        MutableBox implicit = new MutableBox();

        @Mutable MutableBox explicit = new MutableBox();
    }

    @Immutable
    static class ImmutableGenericEx<T extends @Immutable Object> {

        T t;
        @Immutable ImmutableGenericEx(T t) {
            this.t = t;
        }
    }

    @Immutable
    static class ImmutableGenericIm<T extends MutableBox> {
        // :: warning: (implicit.shallow.immutable)
        T t;
        @Immutable ImmutableGenericIm(T t) {
            this.t = t;
        }
    }
}