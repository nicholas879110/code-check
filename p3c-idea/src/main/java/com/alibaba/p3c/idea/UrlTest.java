package com.alibaba.p3c.idea;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * @author zhangliewei
 * @date 2018/1/15 18:51
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class UrlTest {
    public static void main(String[] args) {
        try {
            System.out.println(URLEncoder.encode("http://api.map.baidu.com/geocoder?location=30.551322099999997,104.06379749999999&coord_type=gps&output=json","UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
