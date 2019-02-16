package de.bernd.shandschuh.sparserss.util;

import android.text.TextUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlUtils {

    private static Pattern IMG_PATTERN = Pattern.compile("<img\\s+[^>]*src=\\s*['\"]([^'\"]+)['\"][^>]*>", Pattern.CASE_INSENSITIVE);
    private static String URL_SPACE = "%20";

    public static String getFirstImmage(String strHtml){

        if (!TextUtils.isEmpty(strHtml)) {
            Matcher matcher = IMG_PATTERN.matcher(strHtml);
            while (matcher.find()){
                String s = matcher.group(1).replace(" ", URL_SPACE);
                return s;
            }
        }
        return null;
    }
}
