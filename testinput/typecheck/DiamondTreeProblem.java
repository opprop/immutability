import qual.Immutable;

import java.util.List;
import java.util.ArrayList;

public class DiamondTreeProblem {

    void test1() {
        @Immutable List<String> l = new @Immutable ArrayList<>();
    }

    void test2() {
        @Immutable List<String> l = new @Immutable ArrayList<String>();
    }
}
