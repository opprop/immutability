import org.checkerframework.checker.initialization.qual.UnderInitialization;

public class FieldAssignCase2_3 {
    Object o;
    FieldAssignCase2_3() {
        init();
    }

    void init(@UnderInitialization FieldAssignCase2_3 this) {
        o = new Object();
    }
}
