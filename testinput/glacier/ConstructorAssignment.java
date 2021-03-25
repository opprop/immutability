package glacier;

import qual.Immutable;

public @Immutable class ConstructorAssignment {
    public int x = 3; // static assignment is OK

    ConstructorAssignment() {
        x = 4; // constructor assignment is OK
    }

    void setX() {
        // ::error: (illegal.field.write)
        x = 5;
    }
}

class OtherClass {
    OtherClass() {
        ConstructorAssignment c = new ConstructorAssignment();
        // ::error: (illegal.field.write)
        c.x = 6;
    }
}