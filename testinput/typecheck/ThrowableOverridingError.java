import qual.Readonly;

class A extends Throwable {
    @Override
    public String getMessage(@Readonly A this) {
        return super.getMessage();
    }
}

public class ThrowableOverridingError extends Throwable{
    // getXXX() methods are now defaulted to have @Readonly declared receiver, so
    // override.receiver.invalid is not expected anymore(previously @Mutable
    // didn't override @Readonly)
    @Override public String getMessage() {
        return super.getMessage();
    }
}
