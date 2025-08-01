package com.cskaoyan.duolai.clean.housekeeping.service.impl;

import com.cskaoyan.duolai.clean.common.constants.UserType;
import com.cskaoyan.duolai.clean.common.expcetions.ForbiddenOperationException;
import com.cskaoyan.duolai.clean.common.expcetions.RequestForbiddenException;
import com.cskaoyan.duolai.clean.common.utils.JwtTool;
import com.cskaoyan.duolai.clean.housekeeping.dao.entity.OperatorDO;
import com.cskaoyan.duolai.clean.housekeeping.request.LoginCommand;
import com.cskaoyan.duolai.clean.housekeeping.service.ILoginService;
import com.cskaoyan.duolai.clean.housekeeping.service.IOperatorService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class LoginServiceImpl implements ILoginService {

    @Resource
    private IOperatorService operatorService;
    @Resource
    private JwtTool jwtTool;
    @Resource
    private PasswordEncoder passwordEncoder;

    /**
     * 运营员登录
     *
     * @param loginCommand 运营人员登录请求模型
     * @return token
     */
    @Override
    public String login(LoginCommand loginCommand) {

        // 1. 根据用户名查询数据库中用户的信息
        OperatorDO operatorDO = operatorService.findByUsername(loginCommand.getUsername());

        if(operatorDO == null){
            // 没有查询出指定用户
            throw new RequestForbiddenException("用户名错误！");
        }

        // 2. 校验密码
        if(!passwordEncoder.matches(loginCommand.getPassword(),operatorDO.getPassword())){
            // 密码匹配失败
            throw new RequestForbiddenException("密码错误!");
        }

        // 生成jwt的token字符串
        String token = jwtTool.createToken(operatorDO.getId(), operatorDO.getName(), operatorDO.getAvatar(), UserType.OPERATION);

        return token;
    }
}
