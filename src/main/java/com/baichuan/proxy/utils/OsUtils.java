package com.baichuan.proxy.utils;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * @author kun
 * @date 2020-06-19 16:56
 */
public class OsUtils {
    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static final String ARCH = System.getProperty("sun.arch.data.model");

    public OsUtils() {
    }

    public static int getFreePort(int defaultPort) throws IOException {
        try {
            ServerSocket serverSocket = new ServerSocket(defaultPort);
            Throwable var2 = null;

            int var3;
            try {
                var3 = serverSocket.getLocalPort();
            } catch (Throwable var13) {
                var2 = var13;
                throw var13;
            } finally {
                if (serverSocket != null) {
                    if (var2 != null) {
                        try {
                            serverSocket.close();
                        } catch (Throwable var12) {
                            var2.addSuppressed(var12);
                        }
                    } else {
                        serverSocket.close();
                    }
                }

            }

            return var3;
        } catch (IOException var15) {
            return getFreePort();
        }
    }

    public static int getFreePort() throws IOException {
        ServerSocket serverSocket = new ServerSocket(0);
        Throwable var1 = null;

        int var2;
        try {
            var2 = serverSocket.getLocalPort();
        } catch (Throwable var11) {
            var1 = var11;
            throw var11;
        } finally {
            if (serverSocket != null) {
                if (var1 != null) {
                    try {
                        serverSocket.close();
                    } catch (Throwable var10) {
                        var1.addSuppressed(var10);
                    }
                } else {
                    serverSocket.close();
                }
            }

        }

        return var2;
    }

    public static boolean isBusyPort(int port) {
        boolean ret = true;
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(port);
            ret = false;
        } catch (Exception var12) {
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException var11) {
                    var11.printStackTrace();
                }
            }

        }

        return ret;
    }

    public static boolean isWindows() {
        return OS.indexOf("win") >= 0;
    }

    public static boolean isWindowsXP() {
        return OS.indexOf("win") >= 0 && OS.indexOf("xp") >= 0;
    }

    public static boolean isMac() {
        return OS.indexOf("mac") >= 0;
    }

    public static boolean isUnix() {
        return OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") >= 0;
    }

    public static boolean isSolaris() {
        return OS.indexOf("sunos") >= 0;
    }

    public static boolean is64() {
        return "64".equals(ARCH);
    }

    public static boolean is32() {
        return "32".equals(ARCH);
    }
}
