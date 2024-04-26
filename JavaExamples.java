/// Java Examples
import java.util.*;
import qual.Mutable;

// Mutating immut set, `s` is immut
void foo(Set<String> s) {
  Set<String> new_s = s;
  @Mutable Set<String> new_s1 = s; // ERROR
  new_s.add("x"); // ERROR
}

// Mutating immut set, `new_s` is immut
void foo1(@Mutable Set<String> s) {
  Set<String> new_s = new HashSet<>(s);
  Set<String> new_s1 = s;
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

// Class and its immut members
class Person {
  String name;
  List<String> family;

  Person(String n, List<String> f) {
    this.name = n; // OK
    this.family = f; // OK
    this.family.add("Mom"); // ERROR
  }

  void setName(String n) {
    this.name = n; // ERROR
  }

  @Mutable List<String> getFamily() {
    return family; // ERROR
  }
}

void foo4(Person p) {
  p.name = "Jenny"; // ERROR
  p.family.add("Jenny"); // ERROR
  p.family.getFamily().add("Jenny"); // OK
}

// Class and its mut members
class MutPerson {
  @Mutable String name;
  @Mutable List<String> family;

  Person(String n, List<String> f) {
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

void foo5(MutPerson p) {
  p.name = "Jenny"; // OK
  p.family.add("Jenny"); // OK
  p.family.getFamily().add("Jenny"); // ERROR
}
