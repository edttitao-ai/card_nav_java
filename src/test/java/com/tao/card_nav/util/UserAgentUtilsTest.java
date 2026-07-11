package com.tao.card_nav.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserAgentUtilsTest {

    // ---------- parseBrowser ----------

    @Test
    void parseBrowser_chrome() {
        // 纯 Chrome：UA 只含 Chrome，不含 Edg/OPR
        String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
        assertThat(UserAgentUtils.parseBrowser(ua)).isEqualTo("Chrome");
    }

    @Test
    void parseBrowser_edge_chromeLike() {
        // Edge UA 同时含 Chrome，必须在 Chrome 之前识别
        String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0";
        assertThat(UserAgentUtils.parseBrowser(ua)).isEqualTo("Edge");
    }

    @Test
    void parseBrowser_opera_modernOpr() {
        // 现代 Opera UA 同时含 Chrome + OPR；原 VisitorsController 实现的
        // "Chrome 优先" 会把它误判为 Chrome。修复后预期 "Opera"。
        String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 OPR/106.0.0.0";
        assertThat(UserAgentUtils.parseBrowser(ua)).isEqualTo("Opera");
    }

    @Test
    void parseBrowser_opera_legacyKeyWord() {
        // 老 Opera UA 含 "Opera"
        String ua = "Opera/9.80 (Windows NT 6.1; WOW64) Presto/2.12.388 Version/12.16";
        assertThat(UserAgentUtils.parseBrowser(ua)).isEqualTo("Opera");
    }

    @Test
    void parseBrowser_firefox() {
        String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0";
        assertThat(UserAgentUtils.parseBrowser(ua)).isEqualTo("Firefox");
    }

    @Test
    void parseBrowser_safari_withoutChrome() {
        String ua = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Safari/605.1.15";
        assertThat(UserAgentUtils.parseBrowser(ua)).isEqualTo("Safari");
    }

    @Test
    void parseBrowser_unknown_returnsMobileBrowserDefault() {
        assertThat(UserAgentUtils.parseBrowser(null)).isEqualTo("Mobile Browser");
        assertThat(UserAgentUtils.parseBrowser("Unknown-UA/1.0")).isEqualTo("Mobile Browser");
    }

    // ---------- parseDevice ----------

    @Test
    void parseDevice_pc() {
        String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0";
        assertThat(UserAgentUtils.parseDevice(ua)).isEqualTo("PC端");
    }

    @Test
    void parseDevice_iphone() {
        String ua = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 Version/17.0 Mobile/15E148 Safari/604.1";
        assertThat(UserAgentUtils.parseDevice(ua)).isEqualTo("手机端");
    }

    @Test
    void parseDevice_android() {
        String ua = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36";
        assertThat(UserAgentUtils.parseDevice(ua)).isEqualTo("手机端");
    }

    @Test
    void parseDevice_ipad() {
        String ua = "Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X) AppleWebKit/605.1.15 Version/17.0 Mobile/15E148 Safari/604.1";
        assertThat(UserAgentUtils.parseDevice(ua)).isEqualTo("手机端");
    }

    @Test
    void parseDevice_nullReturnsPc() {
        assertThat(UserAgentUtils.parseDevice(null)).isEqualTo("PC端");
    }
}
