package com.tao.card_nav.controller;

import com.tao.card_nav.entity.VisitorsDo;
import com.tao.card_nav.result.Result;
import com.tao.card_nav.service.VisitorsService;
import com.tao.card_nav.util.ClientIpUtils;
import com.tao.card_nav.util.UserAgentUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@Slf4j
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
            String ip = ClientIpUtils.resolve(request);
            String userAgent = request.getHeader("User-Agent");
            String browser = UserAgentUtils.parseBrowser(userAgent);
            String device = UserAgentUtils.parseDevice(userAgent);

            VisitorsDo visitor = VisitorsDo.builder()
                    .ip(ip)
                    .browser(browser)
                    .device(device)
                    .timestamp(new Date())
                    .build();

            visitorsService.insert(visitor);
            request.getSession(true).setAttribute(VISITOR_KEY, Boolean.TRUE);
        } catch (Exception e) {
            // 访客记录失败不应影响主流程，但要留痕便于排查
            log.warn("记录访客失败", e);
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
}
