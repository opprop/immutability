
class D<T extends Object>{

}

public class TypeVariableUse{
    // :: fixable-error: (type.argument.type.incompatible)
    D<String> d = new D<String>();
}
