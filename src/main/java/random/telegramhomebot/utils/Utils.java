package random.telegramhomebot.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class Utils {

	public static <T> List<T> joinLists(List<T>... lists) {
		return Arrays.stream(lists)
				.filter(Objects::nonNull)
				.flatMap(Collection::stream)
				.collect(Collectors.toList());
	}
}
