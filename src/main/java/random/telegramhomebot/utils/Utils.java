package random.telegramhomebot.utils;

import javax.servlet.http.HttpServletRequest;
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

	public static String getClientIp(HttpServletRequest request) {
		String remoteAddr = "undefined";

		if (request != null) {
			remoteAddr = request.getHeader("X-FORWARDED-FOR");
			if (remoteAddr == null || "".equals(remoteAddr)) {
				remoteAddr = request.getRemoteAddr();
			}
		}
		return remoteAddr;
	}
}
