package random.telegramhomebot.utils;

import random.telegramhomebot.model.Host;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
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

	public static Comparator<Host> comparingByIp() {
		return Comparator.comparing(
				Host::getIp, (s1, s2) -> {
					if (s1 == null) {
						return -1;
					} else if (s2 == null) {
						return 1;
					}
					return s1.compareTo(s2);
				});
	}
}
