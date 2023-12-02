package com.quickdelivery;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by mac on 17/04/2020.
 */
@RestController
@RequestMapping("/fallback")
public class GatewayFallback {

    @GetMapping("/testService")
    public FallbackResponse getAccount() {
        FallbackResponse a = new FallbackResponse();
        a.setMsgCode(505);
        a.setMsg("fallBackMsg");
        return a;
    }

}