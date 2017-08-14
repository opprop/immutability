import qual.Mutable;

public class MutableField {

    //:: error: (field.mutable.forbidden)
    @Mutable Object f;
}