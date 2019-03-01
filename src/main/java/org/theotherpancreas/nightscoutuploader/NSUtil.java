package org.theotherpancreas.nightscoutuploader;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

public class NSUtil {
    public static String formatTime(long time) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return simpleDateFormat.format(new Date(time));
    }

    public static String formatUUID(UUID id) {
        return id.toString().replaceAll("-", "").toLowerCase();
    }

    public static String convertDeltaToDirection(int delta) {
        int changeIn30Minutes = delta * 6;
        if (changeIn30Minutes > 200)
            return "RATE OUT OF RANGE";
        if (changeIn30Minutes > 100)
            return "DoubleUp";
        if (changeIn30Minutes > 75)
            return "SingleUp";
        if (changeIn30Minutes > 50)
            return "FortyFiveUp";
        if (changeIn30Minutes > -50)
            return "Flat";
        if (changeIn30Minutes > -75)
            return "FortyFiveDown";
        if (changeIn30Minutes > -100)
            return "SingleDown";
        else if (changeIn30Minutes > -200)
            return "DoubleDown";
        else
            return "RATE OUT OF RANGE";
    }
}
