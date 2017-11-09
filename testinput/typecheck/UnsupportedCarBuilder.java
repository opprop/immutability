package typecheck;

import qual.Immutable;

/**
 * This builder pattern is NOT supported if the builder internally holds the constructed
 * object rather than having the representation states.
 */
public class UnsupportedCarBuilder {
    class Car {
        private int wheels;
        private String color;

        // :: error: (initialization.fields.uninitialized)
        private @Immutable Car() {
        }

        public String getColor() {
            return color;
        }

        private void setColor(final String color) {
            this.color = color;
        }

        public int getWheels() {
            return wheels;
        }

        private void setWheels(final int wheels) {
            this.wheels = wheels;
        }

        @Override
        public String toString() {
            return "Car [wheels = " + wheels + ", color = " + color + "]";
        }
    }

    private @Immutable Car car;

    public UnsupportedCarBuilder() {
        car = new @Immutable Car();
    }
    public @Immutable Car build() {
        return car;
    }
    public UnsupportedCarBuilder setColor(final String color) {
        // :: error: (method.invocation.invalid)
        car.setColor(color);
        return this;
    }
    public UnsupportedCarBuilder setWheels(final int wheels) {
        // :: error: (method.invocation.invalid)
        car.setWheels(wheels);
        return this;
    }

    public static UnsupportedCarBuilder getBuilder() {
        return new UnsupportedCarBuilder();
    }

}
class A {
    void foo() {
        UnsupportedCarBuilder.Car car = UnsupportedCarBuilder.getBuilder().setColor("red").setWheels(4).build();
    }
}
