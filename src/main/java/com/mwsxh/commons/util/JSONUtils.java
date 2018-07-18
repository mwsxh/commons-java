package com.mwsxh.commons.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public abstract class JSONUtils {

    public static JSONObject safeParseObject(String text) {
        Object obj = safeParse(text);
        if (obj instanceof JSONObject) {
            return (JSONObject) obj;
        }

        return (JSONObject) JSON.toJSON(obj);
    }

    public static Object safeParse(String text) {
        try {
            if (guessJSON(text)) {
                return JSON.parse(text);
            }
        } catch (Exception e) {
            log.error(text, e);
        }
        return null;
    }

    public static boolean guessJSON(String text) {
        String str = StringUtils.trimToEmpty(text);
        return (StringUtils.startsWith(str, "{") && StringUtils.endsWith(str, "}"))
                || (StringUtils.startsWith(str, "[") && StringUtils.endsWith(str, "]"));
    }

}
