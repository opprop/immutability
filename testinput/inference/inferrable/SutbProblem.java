import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SutbProblem {
    void foo() {
        List<String> fields = new ArrayList<String>();
        // Even though addAll() is declared in ArrayList,
        // the stub parser still looks at the direct definition of
        // "fields", so if addAll() doesn't exist in the stub for
        // java.util.List, default for parameter is @Mutable, and
        // would cause inference to fail as Arrays.asList() returns
        // @Immutable list.
        fields.addAll(Arrays.asList("Hi"));
    }
}
