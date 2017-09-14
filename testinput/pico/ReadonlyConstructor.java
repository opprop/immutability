import qual.Readonly;

public class ReadonlyConstructor {

    //:: error: (constructor.invalid)
    @Readonly ReadonlyConstructor() {}
}