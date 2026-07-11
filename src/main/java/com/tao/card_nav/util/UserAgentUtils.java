package com.tao.card_nav.util;

/**
 * User-Agent 解析工具。
 *
 * <p>把浏览器 / 设备识别从 controller 里抽出来，便于复用与单测。
 *
 * <p>当前是基于子串匹配的简易实现，覆盖常见浏览器与移动端标记；
 * 不追求 UA 库（如 UADetector）的覆盖面，只服务当前统计需求。
 */
public final class UserAgentUtils {

    /** 默认设备类型：非移动 UA 时返回 */
    public static final String DEVICE_PC = "PC端";
    /** 默认设备类型：识别为移动端时返回 */
    public static final String DEVICE_MOBILE = "手机端";
    /** 未识别出具体浏览器时返回 */
    public static final String BROWSER_MOBILE_DEFAULT = "Mobile Browser";

    private static final String UA_CHROME = "Chrome";
    private static final String UA_FIREFOX = "Firefox";
    private static final String UA_SAFARI = "Safari";
    private static final String UA_EDG = "Edg";
    private static final String UA_OPERA = "Opera";
    private static final String UA_OPR = "OPR";

    private UserAgentUtils() {}

    /**
     * 从 User-Agent 解析浏览器名。
     *
     * <p>判定顺序：Edge → Opera(OPR) → Firefox → Safari → Chrome。这是因为现代
     * Opera / Edge UA 同时包含 {@code Chrome}，必须先识别它们，否则 Opera 会被
     * 误识别为 Chrome。原 VisitorsController 的实现先判 Chrome，把现代 Opera
     * 永远打成 Chrome，这就是 fix 的源头。
     *
     * @param userAgent 原始 UA，可为 null
     * @return 浏览器标签；未识别时返回 {@link #BROWSER_MOBILE_DEFAULT}
     */
    public static String parseBrowser(String userAgent) {
        if (userAgent == null) {
            return BROWSER_MOBILE_DEFAULT;
        }
        if (userAgent.contains(UA_EDG)) {
            return "Edge";
        }
        if (userAgent.contains(UA_OPERA) || userAgent.contains(UA_OPR)) {
            return UA_OPERA;
        }
        if (userAgent.contains(UA_FIREFOX)) {
            return UA_FIREFOX;
        }
        if (userAgent.contains(UA_SAFARI) && !userAgent.contains(UA_CHROME)) {
            return UA_SAFARI;
        }
        if (userAgent.contains(UA_CHROME)) {
            return UA_CHROME;
        }
        return BROWSER_MOBILE_DEFAULT;
    }

    /**
     * 从 User-Agent 解析设备类型（PC / 手机）。
     *
     * @param userAgent 原始 UA，可为 null
     * @return {@link #DEVICE_PC} 或 {@link #DEVICE_MOBILE}
     */
    public static String parseDevice(String userAgent) {
        if (userAgent == null) {
            return DEVICE_PC;
        }
        String ua = userAgent.toLowerCase();
        // 移动设备检测
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")
                || ua.contains("ipad") || ua.contains("tablet") || ua.contains("ipod")) {
            return DEVICE_MOBILE;
        }
        return DEVICE_PC;
    }
}
