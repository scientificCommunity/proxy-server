package com.baichuan.proxy.utils;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author kun
 * @date 2020-06-19 16:56
 */
public class PathUtils {
    public static String ROOT_PATH;

    public PathUtils() {
    }

    static {
        URL url = PathUtils.class.getResource("/");
        ROOT_PATH = url.getPath();

        try {
            URLConnection connection = url.openConnection();
            if (connection instanceof JarURLConnection) {
                File userDir = new File(System.getProperty("user.dir"));
                File classPathDir = (new File(System.getProperty("java.class.path"))).getAbsoluteFile().getParentFile();
                ROOT_PATH = userDir.getAbsolutePath().length() > classPathDir.getAbsolutePath().length() ? userDir.getAbsolutePath() : classPathDir.getAbsolutePath();
            }
        } catch (Exception var4) {
        }

        if (OsUtils.isWindows() && ROOT_PATH.matches("^/.*$")) {
            ROOT_PATH = ROOT_PATH.substring(1);
        }

        if (ROOT_PATH.matches("^.*[\\\\/]$")) {
            ROOT_PATH = ROOT_PATH.substring(0, ROOT_PATH.length() - 1);
        }

    }
}
