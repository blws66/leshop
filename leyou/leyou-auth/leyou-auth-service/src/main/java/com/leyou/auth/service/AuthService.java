package com.leyou.auth.service;

import com.leyou.auth.client.UserClient;
import com.leyou.auth.common.pojo.UserInfo;
import com.leyou.auth.common.utils.JwtUtils;
import com.leyou.auth.config.JwtProperties;
import com.leyou.user.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

@Service
@EnableConfigurationProperties(JwtProperties.class)
public class AuthService {
    @Autowired
    private UserClient userClient;
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 生成token
     * @param username
     * @param password
     * @return
     */
    public String authentication(String username, String password) {
        //调用远程接口查询用户
        User user = userClient.queryUser(username, password);
        //判断用户是否存在
        if(user==null){
            return null;
        }
        try {
            UserInfo userInfo = new UserInfo();
            userInfo.setId(user.getId());
            userInfo.setUsername(username);
            return JwtUtils.generateToken(userInfo,jwtProperties.getPrivateKey(),jwtProperties.getExpire());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
