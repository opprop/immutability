import qual.Readonly;

public class ReadonlyConstructor {

    //:: error: (consturctor.invalid)
    @Readonly ReadonlyConstructor() {}
}