package random.telegramhomebot.utils.pagination

class PagerModel(
    var totalPages: Int,
    var currentPage: Int,
    var pageSize: Int,
    var pageSizeCookieName: String,
    var pageSizes: IntArray,
    private var buttonsToShow: Int,
    var mapping: String
) {
    var startPage = 0
    var endPage = 0

    init {
        if (buttonsToShow % 2 == 0) throw IllegalArgumentException("Must be an odd value!")
        totalPages = if (totalPages == 0) ++totalPages else totalPages
        val halfPagesToShow = this.buttonsToShow / 2
        if (totalPages <= this.buttonsToShow) {
            startPage = 1
            endPage = totalPages
        } else if (currentPage - halfPagesToShow <= 0) {
            startPage = 1
            endPage = buttonsToShow
        } else if (currentPage + halfPagesToShow == totalPages) {
            startPage = currentPage - halfPagesToShow
            endPage = totalPages
        } else if (currentPage + halfPagesToShow > totalPages) {
            startPage = totalPages - buttonsToShow + 1
            endPage = totalPages
        } else {
            startPage = currentPage - halfPagesToShow
            endPage = currentPage + halfPagesToShow
        }
    }

    override fun toString() = "Pager [startPage=$startPage, endPage=$endPage]"
}
