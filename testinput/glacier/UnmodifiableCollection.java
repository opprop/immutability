import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


interface SCell {};

public class UnmodifiableCollection {
    public Iterator<SCell> getCellIterator() {
        Collection<SCell> cellCollection = null;
        return Collections.unmodifiableCollection(cellCollection).iterator();
    }
}