package day.happy365.shorturlservice.util;

import org.apache.commons.lang3.StringUtils;

public class Base62Util {

    private static final String CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private static final int SCALE = 62;

    private static final int MIN_LENGTH = 6;

    public static String encode(long i) {

        Long val = Long.valueOf(i);
        long absHashCode = i;
        if (val.intValue() < 0) {
            absHashCode = val & Integer.MAX_VALUE;
        }

        StringBuilder stringBuilder = new StringBuilder();
        int remainder;
        while (absHashCode > SCALE - 1) {
            remainder = Long.valueOf(absHashCode % SCALE).intValue();
            stringBuilder.append(CHARS.charAt(remainder));
            absHashCode = absHashCode / SCALE;
        }
        stringBuilder.append(CHARS.charAt(Long.valueOf(absHashCode).intValue()));
        String value = stringBuilder.reverse().toString();
        return StringUtils.leftPad(value, MIN_LENGTH, '0');
    }

}
