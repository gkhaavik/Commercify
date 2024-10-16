package com.gostavdev.commercify.paymentservice.feignclients;

import com.gostavdev.commercify.paymentservice.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(name = "users-service", url = "${user.service.url}")
public interface UserClient {
    @RequestMapping(method = RequestMethod.GET, value = "/users/{id}")
    UserDTO getUserById(@PathVariable Long id);

    @RequestMapping(method = RequestMethod.GET, value = "/auth/me")
    UserDTO loadUser(@RequestHeader("Authorization") String authHeader);
}