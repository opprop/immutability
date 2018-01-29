import java.lang.reflect.Field;

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
