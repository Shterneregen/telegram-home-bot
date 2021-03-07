package random.telegramhomebot.utils.pagination;

import lombok.Data;

@Data
public class PagerModel {

	private int buttonsToShow;
	private int startPage;
	private int endPage;
	private int totalPages;
	private int currentPage;
	private int pageSize;
	private int[] pageSizes;
	private String mapping;
	private String pageSizeCookieName;

	public PagerModel(int totalPages, int currentPage, int pageSize, String pageSizeCookieName, int[] pageSizes, int buttonsToShow, String mapping) {
		this.totalPages = totalPages;
		this.currentPage = currentPage;
		this.pageSize = pageSize;
		this.pageSizeCookieName = pageSizeCookieName;
		this.pageSizes = pageSizes;
		this.mapping = mapping;
		setButtonsToShow(buttonsToShow);

		int halfPagesToShow = this.buttonsToShow / 2;
		if (totalPages <= this.buttonsToShow) {
			startPage = 1;
			endPage = totalPages;
		} else if (currentPage - halfPagesToShow <= 0) {
			startPage = 1;
			endPage = buttonsToShow;
		} else if (currentPage + halfPagesToShow == totalPages) {
			startPage = currentPage - halfPagesToShow;
			endPage = totalPages;
		} else if (currentPage + halfPagesToShow > totalPages) {
			startPage = totalPages - buttonsToShow + 1;
			endPage = totalPages;
		} else {
			startPage = currentPage - halfPagesToShow;
			endPage = currentPage + halfPagesToShow;
		}
	}

	public void setButtonsToShow(int buttonsToShow) {
		if (buttonsToShow % 2 != 0) {
			this.buttonsToShow = buttonsToShow;
		} else {
			throw new IllegalArgumentException("Must be an odd value!");
		}
	}

	@Override
	public String toString() {
		return "Pager [startPage=" + startPage + ", endPage=" + endPage + "]";
	}
}
