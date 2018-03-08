import qual.Immutable;
import qual.Mutable;

@Immutable
public class MethodReceiverNotInhericClassBound {

    // :: error: (method.receiver.incompatible)
   void foo() {}
}
