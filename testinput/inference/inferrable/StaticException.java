import java.util.List;

class StaticException {

    private static class MyException extends RuntimeException {}

    private static interface MyList<T> extends List<T> {}
}
