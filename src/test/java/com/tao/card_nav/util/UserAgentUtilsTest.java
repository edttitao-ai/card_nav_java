package com.tao.card_nav.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link UserAgentUtils} 单测。
 *
 * <p>测的核心契约：
 * <ul>
 *   <li>浏览器识别顺序：Edge → OPR/Opera → Firefox → Safari → Chrome
 *       （因为现代 Edge / Opera UA 都同时含 "Chrome"，必须先识别它们）</li>
 *   <li>设备识别：mobile / android / iphone / ipad / tablet / ipod 任一命中 → 手机端，否则 PC 端</li>
 *   <li>未识别 fallback：浏览器给 "Mobile Browser"、设备给 "PC端"</li>
 * </ul>
 */
class UserAgentUtilsTest {

    // ============================================================
    // parseBrowser
    // ============================================================

    /**
     * 纯 Chrome UA（不含 Edg/OPR）：走 Chrome 分支。
     */
    @Test
    void parseBrowser_chrome() {
        String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
        assertThat(UserAgentUtils.parseBrowser(ua)).isEqualTo("Chrome");
    }

    /**
     * Edge UA 同时含 "Chrome" 与 "Edg"：必须在 Chrome 之前识别 Edge，
     * 否则会被第一分支命中误判为 Chrome。
     */
    @Test
    void parseBrowser_edge_chromeLike() {
        String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0";
        assertThat(UserAgentUtils.parseBrowser(ua)).isEqualTo("Edge");
    }

    /**
     * <b>回归保护</b>：现代 Opera UA 同时含 Chrome + OPR。
     * 原 VisitorsController 实现的 "Chrome 优先" 判定顺序把 Opera 误判为 Chrome（commit 历史 bug）。
     * 修复后判定顺序调整为 OPR/Opera 优先于 Chrome，此用例锁住被修复的路径。
     */
    @Test
    void parseBrowser_opera_modernOpr() {
        String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 OPR/106.0.0.0";
        assertThat(UserAgentUtils.parseBrowser(ua)).isEqualTo("Opera");
    }

    /**
     * 老 Opera UA（含 "Opera" 关键字）：走 Opera 分支。
     */
    @Test
    void parseBrowser_opera_legacyKeyWord() {
        String ua = "Opera/9.80 (Windows NT 6.1; WOW64) Presto/2.12.388 Version/12.16";
        assertThat(UserAgentUtils.parseBrowser(ua)).isEqualTo("Opera");
    }

    /**
     * Firefox UA（含 "Firefox"）：命中 Firefox 分支（Firefox UA 不含 Chrome/Edge 关键字）。
     */
    @Test
    void parseBrowser_firefox() {
        String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0";
        assertThat(UserAgentUtils.parseBrowser(ua)).isEqualTo("Firefox");
    }

    /**
     * Safari UA（含 Safari 但不含 Chrome）：命中 Safari 分支。
     * 关键判定条件 "Safari && !Chrome"，避免被误判为 Chrome。
     */
    @Test
    void parseBrowser_safari_withoutChrome() {
        String ua = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Safari/605.1.15";
        assertThat(UserAgentUtils.parseBrowser(ua)).isEqualTo("Safari");
    }

    /**
     * 未识别 / null UA：返回默认值 "Mobile Browser"。
     * 注意这是历史 VisitorController 的占位语义，统计时与"真实浏览器"区分。
     */
    @Test
    void parseBrowser_unknown_returnsMobileBrowserDefault() {
        assertThat(UserAgentUtils.parseBrowser(null)).isEqualTo("Mobile Browser");
        assertThat(UserAgentUtils.parseBrowser("Unknown-UA/1.0")).isEqualTo("Mobile Browser");
    }

    // ============================================================
    // parseDevice
    // ============================================================

    /**
     * 标准桌面 UA（无 mobile / android / iphone 等标记）：返回 PC 端。
     */
    @Test
    void parseDevice_pc() {
        String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0";
        assertThat(UserAgentUtils.parseDevice(ua)).isEqualTo("PC端");
    }

    /**
     * iPhone UA（含 iPhone）：返回手机端。
     */
    @Test
    void parseDevice_iphone() {
        String ua = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 Version/17.0 Mobile/15E148 Safari/604.1";
        assertThat(UserAgentUtils.parseDevice(ua)).isEqualTo("手机端");
    }

    /**
     * Android UA（含 android + Mobile 关键字）：返回手机端。
     */
    @Test
    void parseDevice_android() {
        String ua = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36";
        assertThat(UserAgentUtils.parseDevice(ua)).isEqualTo("手机端");
    }

    /**
     * iPad UA（含 iPad + Mobile 关键字）：返回手机端。
     * 业务口径：iPad 归入手机端（窄屏体验），如有"平板"语义需求再细分。
     */
    @Test
    void parseDevice_ipad() {
        String ua = "Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X) AppleWebKit/605.1.15 Version/17.0 Mobile/15E148 Safari/604.1";
        assertThat(UserAgentUtils.parseDevice(ua)).isEqualTo("手机端");
    }

    /**
     * null UA：兜底为 PC 端（与浏览器兜底"Mobile Browser"不对称，是历史遗留）。
     */
    @Test
    void parseDevice_nullReturnsPc() {
        assertThat(UserAgentUtils.parseDevice(null)).isEqualTo("PC端");
    }
}