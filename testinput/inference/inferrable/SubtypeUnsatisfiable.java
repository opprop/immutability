import qual.Immutable;

class SubtypeUnsatisfiable {

    void foo() {
        // bug1: out should be @Mutable(solved by overriding annotateInheritedFromClass in PICOInferenceRealTypeFactory)
        // bug2: @Readonly Object [](solved by the same way above)
        System.out.printf("", 2);
    }
}
