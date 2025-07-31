package com.cskaoyan.duolai.clean.housekeeping.service.impl;

import com.cskaoyan.duolai.clean.common.constants.UserType;
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

        return null;
    }
}
