package org.apache.nifi.processors.evtx.parser;

import com.google.common.primitives.UnsignedInteger;

import java.io.IOException;

/**
 * Created by brosander on 6/1/16.
 */
public class NumberUtil {
    public static int intValueExpected(Number number, int expected, String errorMessage, Object... args) throws IOException {
        int result = number.intValue();
        if (result != expected) {
            throw createException(errorMessage, args, expected, result);
        }
        return result;
    }

    public static int intValueMax(UnsignedInteger unsignedInteger, int max, String errorMessage, Object... args) throws IOException {
        if (unsignedInteger.compareTo(UnsignedInteger.valueOf(max)) > 0) {
            throw createException(errorMessage, args, "< " + max, unsignedInteger);
        }
        return unsignedInteger.intValue();
    }

    private static IOException createException(String errorMessage, Object[] args, Object expected, Object actual) {
        return new IOException(String.format(errorMessage, args) + String.format(" Expected {} got {}.", expected, actual));
    }
}
