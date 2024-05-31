package typecheck;

import java.util.*;

import qual.*;

class ImmutablePerson {

    void test(@Immutable List<String> friends, @Immutable List<String> otherFriends) {
        @Immutable Person p = new @Immutable Person("name", 25, friends);
        
        // :: error: (method.invocation.invalid)
        p.setName("NewName");

        // :: error: (illegal.field.write)
        p.name = "NewName";

        // :: error: (illegal.field.write)
        p.friends = otherFriends;

        // :: error: (method.invocation.invalid)
        p.friends.add("NewFriend");

        // :: error: (method.invocation.invalid)
        p.getFriends().add("NewFriend");
    }
}

@ReceiverDependentMutable class Person {
    String name;
    int age;
    List<String> friends;

    public Person(String name, int age, @ReceiverDependentMutable List<String> friends) {
        this.name = name;
        this.age = age;
        this.friends = friends;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getFriends() {
        return friends;
    }
}