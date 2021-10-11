package random.telegramhomebot.utils;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Utils {

    private Utils() {
    }

    @SafeVarargs
    public static <T> List<T> joinLists(@Nullable List<T>... lists) {
        if (lists == null || lists.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.stream(lists)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}
