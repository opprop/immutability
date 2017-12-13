import qual.Readonly;

class A extends Throwable {
    @Override
    public String getMessage(@Readonly A this) {
        return super.getMessage();
    }
}

public class ThrowableOverridingError extends Throwable{

    // :: error: (override.receiver.invalid)
    @Override public String getMessage() {
        return super.getMessage();
    }
}
