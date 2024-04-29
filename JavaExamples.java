/// Java Examples
import java.util.*;
import qual.Immutable;
import qual.Mutable;

public class JavaExamples {
    // Mutating immut set, `s` is immut
    void foo(@Immutable Set<String> s) {
        Set<String> new_s = s; // Flow sensitive will refine this to @Immutable
        @Mutable Set<String> new_s1 = s; // ERROR, type incompatible
        new_s.add("x"); // ERROR
    }

    // Mutating immut set, `new_s` is immut
    void foo1(@Mutable Set<String> s) {
        @Immutable Set<String> new_s = new @Immutable HashSet<>(s);
        Set<String> new_s1 = s; // Flow sensitive will refine this to @Mutable
        new_s.add("x"); // ERROR
        new_s1.add("x"); //OK
    }

    // Mutating mut set
    void foo2(Set<String> s) {
        @Mutable Set<String> new_s = new HashSet<>(s);
        new_s.add("x"); // OK
    }

    // Type parameter mutability
    void foo3(Set<List<String>> s, List<String> l) {
        assert s.get(l) != null;
        List<String> v = s.get(l);
        @Mutable List<String> v2 = s.get(l); // ERROR
        v.add("x"); // ERROR
    }

    void foo4(Person p) {
        p.name = "Jenny"; // ERROR, this should be forbid by compiler by adding final before String
        p.family.add("Jenny"); // ERROR, can not mutate immut list
    }

    void foo5(MutPerson p) {
        p.name = "Jenny"; // OK
        p.family.add("Jenny"); // OK
        p.getFamily().add("Jenny"); // OK
    }
}

// Class and its immut members
class Person {
    String name; // String is default @Immutable, use final to prevent reassignment
    @Immutable List<String> family;

    Person(String n, @Immutable List<String> f) {
        this.name = n; // OK
        this.family = f; // OK
        this.family.add("Mom"); // ERROR
    }

    void setName(String n) {
        this.name = n; // ERROR, this should be forbid by compiler by adding final before String
    }

    @Mutable List<String> getFamily() {
        return family; // ERROR, type incompatible
    }
}

// Class and its mut members
class MutPerson {
    String name; // String is default @Immutable in PICO
    @Mutable List<String> family;

    MutPerson(String n, List<String> f) {
        this.name = n; // OK
        this.family = f; // OK
        this.family.add("Mom"); // OK
    }

    void setName(String n) {
        this.name = n; // OK
    }

    List<String> getFamily() {
        return family; // OK
    }
}
