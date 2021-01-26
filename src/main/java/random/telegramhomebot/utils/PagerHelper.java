package random.telegramhomebot.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.ui.Model;

import java.util.Optional;

public class PagerHelper {

	private static final int BUTTONS_TO_SHOW = 5;
	private static final int[] PAGE_SIZES = {5, 10, 15, 20, 50, 100};

	private static final String SELECTED_PAGE_SIZE_ATTR = "selectedPageSize";
	private static final String PAGE_SIZES_ATTR = "pageSizes";
	private static final String PAGER_ATTR = "pager";
	private static final String MAPPING_ATTR = "mapping";

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

		PagerModel pager = new PagerModel(totalPages, currentPage, BUTTONS_TO_SHOW);

		model.addAttribute(SELECTED_PAGE_SIZE_ATTR, pageSize);
		model.addAttribute(PAGE_SIZES_ATTR, PAGE_SIZES);
		model.addAttribute(PAGER_ATTR, pager);
		model.addAttribute(MAPPING_ATTR, mapping);
	}
}
