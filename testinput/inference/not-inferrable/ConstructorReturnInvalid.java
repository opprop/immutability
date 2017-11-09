import qual.Readonly;

public class ConstructorReturnInvalid {
    Object o;

    @Readonly ConstructorReturnInvalid(Object o) {
        this.o = o;
    }
}
