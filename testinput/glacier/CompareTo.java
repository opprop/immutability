package glacier;

public class CompareTo {

    public void foo() {
        Object val1 = null;
        Object val2 = null;
        // Shouldn't give an error.
        ((Boolean)val1).compareTo((Boolean)val2);
    }
}