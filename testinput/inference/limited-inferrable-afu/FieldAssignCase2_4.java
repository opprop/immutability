import org.checkerframework.checker.initialization.qual.UnderInitialization;

public class FieldAssignCase2_4 {
    Object o;
    FieldAssignCase2_4() {
        init();
    }

    void init(@UnderInitialization FieldAssignCase2_4 this) {
        this.o = new Object();
    }
}
