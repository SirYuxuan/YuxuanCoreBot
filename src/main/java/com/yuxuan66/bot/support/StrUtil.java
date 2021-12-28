package com.yuxuan66.bot.support;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Sir丶雨轩
 * @since 2021/12/28
 */
public class StrUtil extends cn.hutool.core.util.StrUtil {


    public static String format(CharSequence template, Map<?, ?> map,String prefix,String suffix) {
        return format(template, map, true,prefix,suffix);
    }

    public static String format(CharSequence template, Map<?, ?> map, boolean ignoreNull,String prefix,String suffix) {
        if (null == template) {
            return null;
        }
        if (null == map || map.isEmpty()) {
            return template.toString();
        }

        String template2 = template.toString();
        String value;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            value = utf8Str(entry.getValue());
            if (null == value && ignoreNull) {
                continue;
            }
            template2 = replace(template2, prefix+ entry.getKey() + suffix, value);
        }
        return template2;
    }
}
