import qual.Immutable;
import qual.Readonly;

class Acceptor {
    static void accept(@Immutable Object o) {}
}

public class BottomIssue {
    void test() {
        // TODO Should we allow propagation of @Bottom towards String?
        @Readonly Object rowNames = null;
        // TODO Should we give warning here because of refined @Bottom? It will warn if we enforce
        // forbidding @Bottom in Validator
        Acceptor.accept(rowNames);
    }

    void test2(@Readonly Object o) {
        @Readonly Object lo = o;
        //:: error: (argument.type.incompatible)
        Acceptor.accept(lo);
    }
}
