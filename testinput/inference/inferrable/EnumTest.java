enum Kind {
    SOME;
}

public class EnumTest {
    Kind kind;

    EnumTest() {
        kind = Kind.SOME;
        foo(Kind.SOME);
    }

    public static void foo(Kind kind) {}
}
