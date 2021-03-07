package random.telegramhomebot.utils.pagination;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.ui.Model;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;

public class PagerHelper {

	private static final int BUTTONS_TO_SHOW = 5;
	private static final int[] PAGE_SIZES = {5, 10, 15, 20, 50, 100};
	private static final String PAGER_ATTR = "pager";

	public static PageRequest getPageable(
			Optional<Integer> pageSize, int defaultPageSize,
			Optional<Integer> currentPage, int defaultCurrentPage, String currentPageCookieName,
			Optional<String> sortBy, String defaultSortBy,
			Optional<String> direction, String defaultDirection,
			HttpServletRequest request
	) {

		Cookie pageSizeCookie = getCookie(currentPageCookieName, request);
		int evalPageSize = pageSizeCookie != null
				? Integer.parseInt(pageSizeCookie.getValue())
				: pageSize.orElse(defaultPageSize);
		int evalPage = (currentPage.orElse(0) < 1) ? defaultCurrentPage : currentPage.get() - 1;
		String evalSortBy = sortBy.orElse(defaultSortBy);
		String evalDirection = direction.orElse(defaultDirection);

		return PageRequest.of(evalPage, evalPageSize, Sort.by(Sort.Direction.fromString(evalDirection), evalSortBy));
	}

	public static void prepareModelForPager(
			Model model, int totalPages, int currentPage, int pageSize, String pageSizeCookieName, String mapping) {
		model.addAttribute(PAGER_ATTR,
				new PagerModel(totalPages, currentPage + 1, pageSize, pageSizeCookieName, PAGE_SIZES, BUTTONS_TO_SHOW, mapping));
	}

	private static Cookie getCookie(String cookieName, HttpServletRequest request) {
		return Arrays.stream(request.getCookies())
				.filter(cookie -> cookieName.equalsIgnoreCase(cookie.getName()))
				.findFirst().orElse(null);
	}
}
