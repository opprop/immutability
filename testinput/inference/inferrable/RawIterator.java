import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class RawIterator {

    public void build(Collection classes) {

        // On typechecking and inference side, they now have the same behaviour for raw types:
        // "? extends @Mutable Object" is always the default, no matter for typechecking or
        // constraint generation. It was too restrictive to say the type argument is "? extends
        // @Readonly Object", as after this result flows into lhs local variable after a method
        // call, for example, it makes the local variable to be @Readonly. And if a method from
        // bytecode is called which has default @Mutable everything(parameter&declared receiver),
        // then the inference will fail to give solutions and exit. So to make the behaviour
        // consistent and make programs to be inferrable, I used this assumption for wildcard upper
        // bound.
        Iterator iterator = classes.iterator();

        // If PICOInfer detects already-incompatible cast, should it exit? I think no. Incompatible
        // typecast is a warning, it may or may not be an error. If we treat it as error, then
        // programs that have the below line wouldn't be inferred. One legal use case we discussed
        // is to cast @Mutable datastructure to @Immutable if we guarantee that @Mutable reference
        // doesn't leak. So we should continue if incompatible cast happens(@Mutable to @Immutable).

        // Lian: What is a incompatible cast? It seems good to cast to Immutable if casting to String
        //       is already vaild.
        String s = (String)iterator.next();

        // But for cast that involves at least one VariableSlot, PICOInfer tends to give solutions
        // which are comparable. But in the future, we may not even restrict this. Inferring
        // incompatible casts may be dangerous, even though it allows even more flexible casting.
        File nextFile = (File)iterator.next();
        nextFile.isDirectory();
    }
}
