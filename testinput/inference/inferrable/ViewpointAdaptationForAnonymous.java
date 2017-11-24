import qual.ReceiverDependantMutable;

class ViewpointAdaptationForAnonymous {
    @ReceiverDependantMutable ViewpointAdaptationForAnonymous(String s) {}
}

class Test {

    void test1() {
        ViewpointAdaptationForAnonymous v = new ViewpointAdaptationForAnonymous("") {};
    }


    void test2() {
        ViewpointAdaptationForAnonymous v = new ViewpointAdaptationForAnonymous("");
    }
}
