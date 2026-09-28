

package com.bitselect.agent.user.service;

import com.bitselect.agent.user.controller.request.LoginRequest;
import com.bitselect.agent.user.controller.vo.LoginVO;

public interface AuthService {

    LoginVO login(LoginRequest requestParam);

    void logout();
}
