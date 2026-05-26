package com.zx.common.base.utils;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ZhaoXu
 * @date 2021/11/12 13:20
 */
public class StringUtil {
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]*");
    private static final char A = 'A';
    private static final char Z = 'Z';
    private static final char LOWER_A = 'a';
    private static final char LOWER_Z = 'z';


    /**
     * 利用正则表达式判断字符串是否是数字
     *
     * @param str
     * @return
     */
    public static boolean isNumeric(String str) {
        if (Objects.isNull(str) || str.isEmpty()) {
            return false;
        }
        Matcher isNum = NUMBER_PATTERN.matcher(str);
        if (!isNum.matches()) {
            return false;
        }
        return true;
    }

    /**
     * 首字母变小写
     * @param str
     * @return
     */
    public static String firstCharToLowerCase(String str) {
        char firstChar = str.charAt(0);
        if (firstChar >= A && firstChar <= Z) {
            char[] arr = str.toCharArray();
            arr[0] += (LOWER_A - A);
            return new String(arr);
        }
        return str;
    }

    /**
     * 首字母变大写
     * @param str
     * @return
     */
    public static String firstCharToUpperCase(String str) {
        char firstChar = str.charAt(0);
        if (firstChar >= LOWER_A && firstChar <= LOWER_Z) {
            char[] arr = str.toCharArray();
            arr[0] -= (LOWER_A - A);
            return new String(arr);
        }
        return str;
    }
}
