import qual.Immutable;
import qual.Mutable;
import qual.ReceiverDependantMutable;
import qual.Readonly;
import java.util.ArrayList;

class Person {

    protected String name;
    protected int age;
    protected @ReceiverDependantMutable ArrayList<String> friends;

    public Person(String name, int age, ArrayList<String> friends) {
        this.name = name;
        this.age = age;
        this.friends = friends;
    }

    public String getName(Person this) {
        return name;
    }

    public void setName(Person this, String newName) {
        name = newName;
    }

    public int getAge(Person this) {
        return age;
    }

    public ArrayList<String> getFriends(Person this) {
        return friends;
    }
}

public class ComplicatedTest {
    void testImmutability() {
        String name = "tamier";
        int age = 24;
        ArrayList<String> friends = new ArrayList<String>();
        // :: fixable-error: (type.invalid)
        Person p = new @Immutable Person(name, age, friends);
        // :: fixable-error: (method.invocation.invalid)
        p.getName();
        // :: fixable-error: (method.invocation.invalid)
        p.getAge();
        // :: fixable-error: (method.invocation.invalid)
        p.getFriends();
    }

    void testMutability() {
        String name = "tamier";
        int age = 24;
        ArrayList<String> friends = new ArrayList<String>();
        Person p = new Person(name, age, friends);
        String newName = "newName";
        p.getName();
        p.getAge();

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

    void liftGettersToReadonly(@Readonly Person p) {
        // :: fixable-error: (method.invocation.invalid)
        p.getName();
        // :: fixable-error: (method.invocation.invalid)
        p.getAge();
        // :: fixable-error: (method.invocation.invalid)
        p.getFriends();
    }
}
