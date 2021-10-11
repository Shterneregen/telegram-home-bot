package random.telegramhomebot.utils

object Utils {
    @SafeVarargs
    fun <T> joinLists(vararg lists: List<T>?): List<T> =
        if (lists == null || lists.isEmpty()) emptyList() else lists.filterNotNull().flatten()
}
