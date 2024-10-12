package com.gostavdev.commercify.orderservice.dto;

import java.util.Date;
import java.util.List;

public record UserDTO(
        Long userId,
        String email,
        String firstName,
        String lastName,
        Date createdAt,
        List<String> roles) {
}
