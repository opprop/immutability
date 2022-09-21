import qual.Immutable;
import qual.Mutable;

@Immutable
public class MethodReceiverNotInhericClassBound {

    // :: error: (method.receiver.incompatible)  :: error: (type.invalid.annotations.on.use)
   void bar(@Mutable MethodReceiverNotInhericClassBound this) {}
}
