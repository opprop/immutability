import qual.Immutable;
import qual.Mutable;
import qual.PolyImmutable;
import qual.Readonly;
import java.util.ArrayList;

class Person {

    protected String name;
    protected int age;
    protected  @PolyImmutable ArrayList<String> friends;

    public @PolyImmutable Person(String name, int age, @PolyImmutable ArrayList<String> friends) {
        this.name = name;
        this.age = age;
        this.friends = friends;
    }

    public String getName(@Readonly Person this) {
        return name;
    }

    public void setName(@Mutable Person this, String newName) {
        name = newName;
    }

    public int getAge(@Readonly Person this) {
        return age;
    }

    public @PolyImmutable ArrayList<String> getFriends(@PolyImmutable Person this) {
        return friends;
    }
}

public class ComplicatedTest {

    void testImmutability() {
        String name = "tamier";
        int age = 24;
        @Immutable ArrayList<String> friends = new @Immutable ArrayList<String>();
        @Immutable Person p = new @Immutable Person(name, age, friends);
        String newName = "newName";
        //:: error: (method.invocation.invalid)
        p.setName(newName);
        //:: error: (method.invocation.invalid)
        p.friends.add("newFriend");
        //:: error: (method.invocation.invalid)
        p.getFriends().add("newFriend");// In the error message, it's @Readonly(top) if polymorphism fails to resolve
        //:: error: (illegal.field.write)
        p.name = newName;
        //:: error: (illegal.field.write)
        p.age = 27;
    }

    void testMutability() {
        String name = "tamier";
        int age = 24;
        @Mutable ArrayList<String> friends = new @Mutable ArrayList<String>();
        @Mutable Person p = new @Mutable Person(name, age, friends);
        String newName = "newName";
        // Allow because p is @Mutable
        p.setName(newName);
        // Allow because p is @Mutable
        p.friends.add("newFriend");
        // Allow because p is @Mutable
        p.getFriends().add("newFriend");
        // Allow because p is @Mutable
        p.name = newName;
        // Allow because p is @Mutable
        p.age = 27;
    }
}
