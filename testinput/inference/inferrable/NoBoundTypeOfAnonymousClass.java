import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.lang.Double;

// This testcase is realted to StrangeReadonly, see its documentation for details.
public class NoBoundTypeOfAnonymousClass {

    public Future<Double> foo(ExecutorService executor) {
        // The bug was caused by not being able to get VarAnnot on "new Callable<Double>", and
        // InferenceViewpointAdapter wasn't able to replace type variable with type argument,
        // and method overriding rule generates subtype constraint: overriding method return
        // is subtype of overridden method return type, which is "T extends @Readonly Object
        // super @Bottom null".
        return executor.submit(new Callable<Double>() {
            @Override
            public Double call() throws Exception {
                return Double.valueOf(1.0);
            }
        });
    }
}
