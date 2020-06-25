package com.baichuan.proxy.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author tangkun
 * @date 2019-07-13
 */
@Slf4j
public class RegUtils {

    /**
     * 匹配第一个符合条件的
     *
     * @param inputStr 待匹配字符串
     */
    public static String matchOne(String reg, String inputStr, Integer index) {
        String rtnStr = "";
        Matcher matcher = getMatcher(reg, inputStr);
        while (matcher.find()) {
            //group内的值代表匹配正贼表达式里第几组（）里的内容  如<h1[^>]*>([^<]*)</h1> 取1代表匹配<h1></h1>标签中间的内容
            try {
                rtnStr = matcher.group(index);
            } catch (IndexOutOfBoundsException e) {
                log.error("reg:{},e:", reg, e);
            }
            if (StringUtils.isNotBlank(rtnStr)) {
                return rtnStr;
            }
        }
        return rtnStr;
    }

    public static List<String> matchOne(String reg, String inputStr, String... groupNames) {
        List<String> rtnStr = new ArrayList<String>();
        Matcher matcher = getMatcher(reg, inputStr);
        while (matcher.find()) {
            try {
                for (String groupName : groupNames) {
                    String s = matcher.group(groupName);
                    rtnStr.add(s);
                }
            } catch (IndexOutOfBoundsException e) {
                log.error("reg:{},e:", reg, e);
            }
        }
        return rtnStr;
    }

    /**
     * 匹配并返回符合条件的List列表,index取0
     */
    public static List<String> listMatchAll(String reg, String inputStr) {
        return listMatch(reg, inputStr, 0);
    }

    public static List<String> listMatch(String reg, String inputStr, Integer index) {
        List<String> list = new ArrayList<>();
        Matcher matcher = getMatcher(reg, inputStr);
        while (matcher.find()) {
            list.add(matcher.group(index));
        }
        return list;
    }

    /**
     * 返回set,用作去重
     */
    public static Set<String> listMatchDistinct(String reg, String inputStr, Integer index) {
        Set<String> set = new HashSet<>();
        Matcher matcher = getMatcher(reg, inputStr);
        while (matcher.find()) {
            set.add(matcher.group(index));
        }
        return set;
    }

    public static List<String> listMatchFromList(String reg, List<String> inputList) {
        List<String> list = new ArrayList<>();
        inputList.forEach(s -> {
            Matcher matcher = getMatcher(reg, s);
            while (matcher.find()) {
                list.add(matcher.group());
            }
        });
        return list;
    }


    private static Matcher getMatcher(String reg, String inputStr) {
        Pattern pattern = Pattern.compile(reg);
        return pattern.matcher(inputStr);
    }
}
