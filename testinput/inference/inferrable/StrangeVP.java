import java.lang.reflect.Field;

// @skip-test until there is way to filter out @Bottom solution on wildcard lower bound
public class StrangeVP {
    static {
        Exception exception = null;
        try {
            Class<?> uc = Class.forName("sun.misc.Unsafe");
        } catch (Exception e) {
            exception = e;
        }
    }
}
