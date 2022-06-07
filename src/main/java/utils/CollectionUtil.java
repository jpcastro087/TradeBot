package utils;

import java.util.Collection;

public class CollectionUtil {
    public static boolean isNullOrEmpty(Collection<?> list) {
        return !(list != null && !list.isEmpty());
    }
}
