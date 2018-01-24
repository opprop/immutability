
class D<T extends Object>{

}

public class TypeVariableUse{
    // :: fixable-error: (type.argument.type.incompatible)
    D<String> D = new D<String>();

}
