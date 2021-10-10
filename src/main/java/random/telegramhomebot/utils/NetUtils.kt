package random.telegramhomebot.utils

import random.telegramhomebot.db.model.Host
import java.util.regex.Pattern
import javax.servlet.http.HttpServletRequest

class NetUtils {

    companion object {
        private val UNDEFINED = "undefined"

        @JvmStatic
        fun getClientIp(request: HttpServletRequest?): String = request?.getHeader("X-Forwarded-For")
            ?: request?.getHeader("Proxy-Client-IP")
            ?: request?.getHeader("WL-Proxy-Client-IP")
            ?: request?.getHeader("HTTP_CLIENT_IP")
            ?: request?.getHeader("HTTP_X_FORWARDED_FOR")
            ?: request?.remoteAddr
            ?: UNDEFINED

        fun comparingByIp(): Comparator<Host?> = Comparator { host1, host2 ->
            val ip1 = host1?.ip
            val ip2 = host2?.ip

            if (ip1 == null && ip2 == null) return@Comparator 0
            if (ip1 == null) return@Comparator -1
            if (ip2 == null) return@Comparator 1

            val aOct: List<Int> = try {
                ip1.split(".").map { it.toInt() }
            } catch (e: Exception) {
                return@Comparator 1
            }
            val bOct: List<Int> = try {
                ip2.split(".").map { it.toInt() }
            } catch (e: Exception) {
                return@Comparator -1
            }

            var result = 0
            var i = 0
            while (i < aOct.size && i < bOct.size) {
                result = aOct[i].compareTo(bOct[i])
                if (result != 0) return@Comparator result
                i++
            }
            return@Comparator result
        }

        @JvmStatic
        fun validateMac(mac: String?): Boolean {
            if (mac == null) return false
            val pattern = Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")
            val matcher = pattern.matcher(mac)
            return matcher.find()
        }
    }
}
