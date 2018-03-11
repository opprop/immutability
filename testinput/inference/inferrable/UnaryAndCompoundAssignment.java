import qual.*;

public class UnaryAndCompoundAssignment {

    int counter = 0;

    public static void main(String[] args) {
        UnaryAndCompoundAssignment t = new UnaryAndCompoundAssignment();
        t.next();
    }

    // Should inser an explicit @Mutable
    public void next(UnaryAndCompoundAssignment this) {
        counter++;
        counter += 5;
    }
}
