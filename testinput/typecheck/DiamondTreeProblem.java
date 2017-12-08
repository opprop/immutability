import qual.Immutable;

import java.util.List;
import java.util.ArrayList;

public class DiamondTreeProblem {

    void test1() {
        // TODO This is WRONG even though test passed! Explicit @Immutable annotation
        // on new instance creation is ignored and @Mutable is defaulted!
        // :: error: (assignment.type.incompatible)
        @Immutable List<String> l = new @Immutable ArrayList<>();
    }

    void test2() {
        @Immutable List<String> l = new @Immutable ArrayList<String>();
    }
}
