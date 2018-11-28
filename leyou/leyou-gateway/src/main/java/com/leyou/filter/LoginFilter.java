package com.leyou.filter;

import com.leyou.auth.common.utils.JwtUtils;
import com.leyou.common.utils.CookieUtils;
import com.leyou.config.AllowPaths;
import com.leyou.config.JwtProperties;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CookieValue;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Component
@EnableConfigurationProperties({JwtProperties.class, AllowPaths.class})
public class LoginFilter extends ZuulFilter{
    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private AllowPaths allowPaths;

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 10;
    }

    @Override
    public boolean shouldFilter() {
        //获取白名单
        List<String> pathsList = this.allowPaths.getAllowPaths();
        //获取请求路径
        RequestContext context = RequestContext.getCurrentContext();
        String requestURL = context.getRequest().getRequestURL().toString();
        for (String path : pathsList) {
            if (requestURL.contains(path)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Object run() throws ZuulException {
        RequestContext context = RequestContext.getCurrentContext();
        HttpServletRequest request = context.getRequest();
        String token = CookieUtils.getCookieValue(request, jwtProperties.getCookieName());
        try {
            //验证token通过什么都不做
            JwtUtils.getInfoFromToken(token,jwtProperties.getPublicKey());
        } catch (Exception e) {
            //验证不通过拦截
            context.setSendZuulResponse(false);
            context.setResponseStatusCode(HttpStatus.SC_UNAUTHORIZED);
            e.printStackTrace();
        }
        return null;
    }
}
