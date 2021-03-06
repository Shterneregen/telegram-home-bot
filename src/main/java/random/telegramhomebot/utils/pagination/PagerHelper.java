package random.telegramhomebot.utils.pagination;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.ui.Model;

import java.util.Optional;

public class PagerHelper {

	private static final int BUTTONS_TO_SHOW = 5;
	private static final int[] PAGE_SIZES = {5, 10, 15, 20, 50, 100};
	private static final String PAGER_ATTR = "pager";

	public static PageRequest getPageable(
			Optional<Integer> pageSize, int defaultPageSize,
			Optional<Integer> currentPage, int defaultCurrentPage,
			Optional<String> sortBy, String defaultSortBy,
			Optional<String> direction, String defaultDirection
	) {
		int evalPageSize = pageSize.orElse(defaultPageSize);
		int evalPage = (currentPage.orElse(0) < 1) ? defaultCurrentPage : currentPage.get() - 1;
		String evalSortBy = sortBy.orElse(defaultSortBy);
		String evalDirection = direction.orElse(defaultDirection);

		return PageRequest.of(evalPage, evalPageSize, Sort.by(Sort.Direction.fromString(evalDirection), evalSortBy));
	}

	public static void prepareModelForPager(
			Model model, int pageSize, int totalPages, int currentPage, String mapping) {
		model.addAttribute(PAGER_ATTR,
				new PagerModel(totalPages, currentPage + 1, pageSize, BUTTONS_TO_SHOW, PAGE_SIZES, mapping));
	}
}
