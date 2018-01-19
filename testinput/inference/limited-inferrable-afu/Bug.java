package bug;

// https://github.com/typetools/annotation-tools/issues/154
public class Bug<T extends Bound> {
    Bound b;
    Bug() {}
}
//../annotation-tools/annotation-file-utilities/scripts/insert-annotations-to-source -v -d . testinput/inference/limited-inferrable-afu/0.jaif testinput/inference/limited-inferrable-afu/Bound.java testinput/inference/limited-inferrable-afu/Bug.java
