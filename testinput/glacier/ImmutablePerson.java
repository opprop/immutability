// @skip-test
// Does not apply to PICO: shallow immutable support

package glacier;

import qual.Immutable;

import java.util.Date;

/*
@Immutable class ImmutableDate {
	double secondsSinceEpoch;

	void setSeconds(double s) {
		secondsSinceEpoch = s; // Should error!
	}
}
public @Immutable class ImmutablePerson {
	public ImmutablePerson() {
		birthdate = new ImmutableDate();

	}

	ImmutableDate birthdate;

	public void test() {

	}
}
class Person {
	String name;
}
*/


@Immutable public class ImmutablePerson {
    // Date is mutable
    // :: error: glacier.mutable.invalid
    Date birthdate;

    public ImmutablePerson() {

    }

    public void aMethod() {

    }
}