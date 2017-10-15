import qual.Bottom;
import qual.Immutable;
import qual.Readonly;

class Acceptor {
    // TODO @Bottom here should raise error, no problem
    static void accept(@Bottom Object o) {}
}

public class BottomIssue {
    void test1() {
        // TODO Should we allow propagation of @Bottom towards declared type?
        @Readonly Object rowNames = null;
        // TODO Should we give warning here because of refined @Bottom? It will warn if we enforce
        // forbidding @Bottom in Validator
        Acceptor.accept(rowNames);
    }

    void test2() {
        String s = null;
        Acceptor.accept(s);
    }

    void test3(@Readonly Object o) {
        @Readonly Object lo = o;
        //:: error: (argument.type.incompatible)
        Acceptor.accept(lo);
    }
}
