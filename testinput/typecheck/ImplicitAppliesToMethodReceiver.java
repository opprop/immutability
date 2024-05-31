package typecheck;

import qual.Immutable;
import qual.Mutable;

public class ImplicitAppliesToMethodReceiver {
    void foo() {
        double delta = Double.valueOf(1.0);
    }
}
