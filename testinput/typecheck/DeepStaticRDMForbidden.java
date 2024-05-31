import qual.ReceiverDependentMutable;

public class DeepStaticRDMForbidden {

    // :: error: (static.receiverdependentmutable.forbidden)
    static <T extends @ReceiverDependentMutable Object> void foo(T p) {
        // :: error: (static.receiverdependentmutable.forbidden)
        p = null;
    }
}
