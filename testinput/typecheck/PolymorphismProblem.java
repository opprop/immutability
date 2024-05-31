package typecheck;

import qual.Readonly;
import qual.Immutable;
import qual.PolyMutable;
import qual.ReceiverDependentMutable;

class A{
    @ReceiverDependentMutable Object read(@Readonly A this, @PolyMutable Object p) {
        return new @ReceiverDependentMutable Object();
    }
}

class PolymorphismProblem {
   @PolyMutable Object foo(@PolyMutable A a) {
       // Typecheck now. Only when the declared type is @PolyMutable, after viewpoint adadptation,
       // it becomes @SubsitutablePolyMutable, and then will be resolved by QualifierPolymorphism
       // Note: viewpoint adaptation(ATF) happens before QualfierPolymorphism(GATF) in current implementation
       @PolyMutable Object result = a.read(new @Immutable Object());
       return result;
   }
}
