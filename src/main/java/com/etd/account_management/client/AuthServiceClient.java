package com.etd.account_management.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "auth-service", url = "${auth.service.base_url}")
public interface AuthServiceClient {

    @GetMapping("auth/blacklist/check")
    Boolean isBlacklisted(@RequestParam("token") String token);

}
