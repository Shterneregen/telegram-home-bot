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
		return (host1, host2) -> {
			String ip1 = host1.getIp();
			String ip2 = host2.getIp();
			if (ip1 == null && ip2 == null) {
				return 0;
			}
			if (ip1 == null) {
				return -1;
			}
			if (ip2 == null) {
				return 1;
			}

			int[] aOct;
			try {
				aOct = Arrays.stream(ip1.split("\\.")).mapToInt(Integer::parseInt).toArray();
			} catch (Exception e) {
				return -1;
			}
			int[] bOct;
			try {
				bOct = Arrays.stream(ip2.split("\\.")).mapToInt(Integer::parseInt).toArray();
			} catch (Exception e) {
				return 1;
			}

			int r = 0;
			for (int i = 0; i < aOct.length && i < bOct.length; i++) {
				r = Integer.compare(aOct[i], bOct[i]);
				if (r != 0) {
					return r;
				}
			}
			return r;
		};
	}
}
