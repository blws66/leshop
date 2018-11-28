package com.leyou.user.service;

import com.leyou.common.utils.NumberUtils;
import com.leyou.user.mapper.UserMapper;
import com.leyou.user.pojo.User;
import com.leyou.user.utils.CodecUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private AmqpTemplate amqpTemplate;
    static final Logger logger = LoggerFactory.getLogger(UserService.class);
    static final String KEY_PREFIX="user:verify:";

    public Boolean checkUser(String data, Integer type) {
        User user = new User();
        switch (type){
            case 1:
                user.setUsername(data);break;
            case 2:
                user.setPhone(data);break;
            default:
                return null;
        }
        return userMapper.selectCount(user)==1;
    }

    public Boolean sendVerifyCode(String phone) {
        if(phone==null){
            return false;
        }
        //生成验证码
        String code = NumberUtils.generateCode(6);
        try {
            //保存到redis
            redisTemplate.opsForValue().set(KEY_PREFIX+phone,code,5, TimeUnit.MINUTES);
            //发送消息
            Map<String, String> msg = new HashMap<>();
            msg.put("phone",phone);
            msg.put("code",code);
            amqpTemplate.convertAndSend("LEYOU-SMS-EXCHANGE","sms.verifycode",msg);
            return true;
        } catch (AmqpException e) {
            logger.error("发送短信失败。phone：{}， code：{}", phone, code);
            return false;
        }
    }

    public Boolean register(User user, String code) {
        //取出验证码并比较是否相等
        String rediscode = redisTemplate.opsForValue().get(KEY_PREFIX+user.getPhone());
        if(!StringUtils.equals(code,rediscode)){
            return null;
        }
        //生成盐
        String salt = CodecUtils.generateSalt();
        user.setSalt(salt);
        //加密并保存
        user.setPassword(CodecUtils.md5Hex(user.getPassword(),salt));
        user.setId(null);
        user.setCreated(new Date());
        return this.userMapper.insertSelective(user)==1;
    }

    public User queryUser(String username, String password) {
        //根据用户名获取用户信息
        User record = new User();
        record.setUsername(username);
        User user = this.userMapper.selectOne(record);
        if(user==null){
            return null;
        }
        //获取盐并比较已经加密的密码
        if(!StringUtils.equals(user.getPassword(),CodecUtils.md5Hex(password,user.getSalt()))){
            return null;
        }
        return user;
    }
}
