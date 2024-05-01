import qual.*;

public class TheClass {
    public @PolyMutable String f;
    public static String sf;
    public Object of = null;
    public static int sof = 1;

    //    public TheClass() {
    //        f = "hello";
    //    }

    //    public TheClass(String p1) {
    //        this.f = p1;
    //    }

    public String doSomethingElse(@PolyMutable TheClass this, String p1, Object p2, int p4) {
        String s1 = p1;
        String s2 = s1;
        String s3 = s2 + " world";
        return s3;
    }

    public String doSomething(@Readonly TheClass this) {
        String s = doSomethingElse(f, null, 1);
        return s;
    }

    //    public String doMore() {
    //        String d1 = "hello";
    //        TheClass c = new TheClass();
    //        f = d1;
    //        c.f = d1;
    //        TheClass c2 = new TheClass();
    //        c = c2;
    //        return c.f;
    //    }

    //    public String toString() {
    //        return f;
    //    }

    //    public boolean equals(Object o) {
    //        return true;
    //    }
}

class AnotherClass extends TheClass {

    //    @Override
    //    public void doSomething(){
    //        String[] localA2 = testArrays2();
    //        String[] localA3 = localA2;
    //    }

    //    String[] fieldA;
    //
    //    public void testArrays() {
    //        String[] localA = this.fieldA;
    //    }

    //    public String[] testArrays2() {
    //        return fieldA;
    //    }

}
