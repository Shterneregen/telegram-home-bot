package random.telegramhomebot.utils;

import random.telegramhomebot.model.Host;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isBlank;

public abstract class NetUtils {

    private static final String UNDEFINED = "undefined";

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return UNDEFINED;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (isBlank(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (isBlank(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (isBlank(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (isBlank(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (isBlank(ip)) {
            ip = request.getRemoteAddr();
        }
        if (isBlank(ip)) {
            ip = UNDEFINED;
        }
        return ip;
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

    public static boolean validateMac(String mac) {
        Pattern p = Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$");
        Matcher m = p.matcher(mac);
        return m.find();
    }
}
