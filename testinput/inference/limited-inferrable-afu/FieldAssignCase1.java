import qual.Assignable;

public class FieldAssignCase1 {
    @Assignable Object o;
    FieldAssignCase1(Object o) {
        // :: fixable-error: (illegal.field.write)
        this.o = o;
    }
}
