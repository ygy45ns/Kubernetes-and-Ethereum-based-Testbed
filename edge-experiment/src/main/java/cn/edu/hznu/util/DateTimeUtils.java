package cn.edu.hznu.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateTimeUtils {

    public static String getFlag() {
        ZoneId zoneId = ZoneId.of("Asia/Shanghai");
        var dateTime = LocalDateTime.now(zoneId);
        var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm");
        return formatter.format(dateTime);
    }
}
