public class LocalVariableReturnedWithoutBeingInitialized {

    LocalVariableReturnedWithoutBeingInitialized foo() {
        LocalVariableReturnedWithoutBeingInitialized t = null;
        if (true) {
            t = new LocalVariableReturnedWithoutBeingInitialized();
        }
        // :: error: (return.type.incompatible)
        return t;
    }
}
