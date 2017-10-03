class Word {
    Object get(int i) {
        return null;
    }
}
public class Primitive3 {

    void foo(Word words) {
        String[] params = {};
        params[0] = (String) words.get(0);
    }
}