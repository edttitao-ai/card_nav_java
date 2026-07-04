package com.tao.card_nav.aspect;

import com.tao.card_nav.entity.CardsDo;
import com.tao.card_nav.service.CardLogsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;



@Aspect
@Component
@RequiredArgsConstructor
public class CardLogAspect {

    private final CardLogsService cardLogsService;

    // 拦截 CardsService 的所有 public 方法
    @Pointcut("execution(* com.tao.card_nav.service.CardsService.*(..))")
    public void cardsServiceMethods() {}

    @Around("cardsServiceMethods()")
    public Object logCardOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        // 获取卡片 ID
        Long cardId = null;
        CardsDo cardDo = null;
        
        // 从参数中提取 cardId
        for (Object arg : args) {
            if (arg instanceof Long) {
                cardId = (Long) arg;
            } else if (arg instanceof CardsDo) {
                cardDo = (CardsDo) arg;
                cardId = cardDo.getId();
            }
        }
        
        // 判断操作类型
        String action = getActionType(methodName);
        if (action == null) {
            // 不是增删改操作，直接执行
            return joinPoint.proceed();
        }
        
        // 获取操作者 IP（从当前请求上下文获取）
        String operatorIp = getClientIp();
        
        // 执行原方法
        Object result = joinPoint.proceed();
        
        // 如果是新增操作，从返回值获取 cardId
        if ("INSERT".equals(action) && result instanceof CardsDo) {
            cardId = ((CardsDo) result).getId();
        }
        
        // 记录日志
        if (cardId != null) {
            try {
                cardLogsService.logAction(cardId, action, operatorIp);
            } catch (Exception e) {
                // 日志记录失败不影响主业务，但要用 logger 记录，别只 printStackTrace
                org.slf4j.LoggerFactory.getLogger(CardLogAspect.class)
                        .error("记录卡片操作日志失败: cardId={}, action={}", cardId, action, e);
            }
        }
        
        return result;
    }

    /**
     * 根据方法名判断操作类型
     */
    private String getActionType(String methodName) {
        if (methodName.startsWith("add") || methodName.startsWith("create")) {
            return "INSERT";
        } else if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            return "DELETE";
        } else if (methodName.startsWith("update") || methodName.startsWith("modify")) {
            return "UPDATE";
        }
        return null;
    }

    /**
     * 获取客户端 IP
     */
    private String getClientIp() {
        try {
            // 尝试从 RequestContextHolder 获取
            HttpServletRequest request = getCurrentRequest();
            if (request != null) {
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
        } catch (Exception e) {
            // 忽略
            e.printStackTrace();
        }
        return "127.0.0.1";
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            org.springframework.web.context.request.RequestContextHolder
                .getRequestAttributes();
            // 使用 Spring MVC 的 RequestContextHolder
            return ((org.springframework.web.context.request.ServletRequestAttributes) 
                org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes())
                .getRequest();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
