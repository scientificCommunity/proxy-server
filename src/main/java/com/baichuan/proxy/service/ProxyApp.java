package com.baichuan.proxy.service;

import com.baichuan.proxy.Test;

/**
 * @author kun
 * @date 2020-06-11 17:54
 */
public class ProxyApp {
    public static void main(String[] args) {
        try {
            Test.clientTest();
        }catch (Exception e) {
            System.out.println("连接谷歌异常");
        }

        new ProxyServer().init().start(9999);
    }
}
