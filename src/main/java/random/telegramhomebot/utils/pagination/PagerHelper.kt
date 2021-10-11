package random.telegramhomebot.utils.pagination

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.ui.Model
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest

object PagerHelper {

    private const val BUTTONS_TO_SHOW = 5
    private val PAGE_SIZES = intArrayOf(5, 10, 15, 20, 50, 100)
    private const val PAGER_ATTR = "pager"

    fun getPageable(
        pageSize: Int,
        currentPage: Int,
        sortBy: String?,
        direction: String,
        pageCookieName: String,
        request: HttpServletRequest?
    ): PageRequest {
        val cookiePageSize = getCookie(pageCookieName, request)?.value?.toInt() ?: pageSize
        return PageRequest.of(currentPage - 1, cookiePageSize, Sort.by(Sort.Direction.fromString(direction), sortBy))
    }

    fun prepareModelForPager(
        model: Model,
        totalPages: Int,
        currentPage: Int,
        pageSize: Int,
        pageSizeCookieName: String,
        mapping: String
    ) {
        model.addAttribute(
            PAGER_ATTR,
            PagerModel(totalPages, currentPage + 1, pageSize, pageSizeCookieName, PAGE_SIZES, BUTTONS_TO_SHOW, mapping)
        )
    }

    private fun getCookie(cookieName: String, request: HttpServletRequest?): Cookie? =
        request?.cookies?.firstOrNull { cookieName.equals(it?.name, ignoreCase = true) }
}
