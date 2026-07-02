package com.tao.card_nav.controller;

import com.tao.card_nav.aspect.VisitorInterceptor;
import com.tao.card_nav.entity.VisitorsDo;
import com.tao.card_nav.result.Result;
import com.tao.card_nav.mapper.VisitorsDoMapper;
import com.tao.card_nav.service.VisitorsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/visitors")
@RequiredArgsConstructor
public class VisitorsController {

    private final VisitorsService visitorsService;
    private final HttpServletRequest request;

    private static final String VISITOR_KEY = "VISITOR_RECORDED";

    /**
     * 记录一次访问（仅首页调用，同一会话只记录一次）
     */
    @PostMapping
    public Result<Void> recordVisit() {
        // 同一会话只记录一次
        if (request.getSession(false) != null &&
            Boolean.TRUE.equals(request.getSession(false).getAttribute(VISITOR_KEY))) {
            return Result.success();
        }

        try {
            String ip = getClientIp(request);
            String browser = request.getHeader("User-Agent");
            String[] browsers = {"Chrome", "Firefox", "Safari", "Edge", "Opera", "Mobile Browser"};
            if (browser == null) {
                browser = browsers[(int) (Math.random() * browsers.length)];
            } else {
                browser = parseBrowser(browser);
            }
            String[] devices = {"PC端", "手机端"};
            String device = devices[(int) (Math.random() * devices.length)];

            VisitorsDo visitor = VisitorsDo.builder()
                    .ip(ip)
                    .browser(browser)
                    .device(device)
                    .timestamp(new Date())
                    .build();

            visitorsService.insert(visitor);
            request.getSession(true).setAttribute(VISITOR_KEY, Boolean.TRUE);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Result.success();
    }

    /**
     * 获取访客日志列表
     */
    @GetMapping
    public Result<List<VisitorsDo>> getVisitors(@RequestParam(defaultValue = "50") int limit) {
        return Result.success(visitorsService.getVisitors(limit));
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String parseBrowser(String userAgent) {
        if (userAgent.contains("Chrome") && !userAgent.contains("Edg")) {
            return "Chrome";
        } else if (userAgent.contains("Firefox")) {
            return "Firefox";
        } else if (userAgent.contains("Safari") && !userAgent.contains("Chrome")) {
            return "Safari";
        } else if (userAgent.contains("Edg")) {
            return "Edge";
        } else if (userAgent.contains("Opera") || userAgent.contains("OPR")) {
            return "Opera";
        }
        return "Mobile Browser";
    }
}