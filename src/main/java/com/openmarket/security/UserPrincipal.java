package com.openmarket.security;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

/**
 * User principal for security context
 */
@Data
@AllArgsConstructor
public class UserPrincipal {
    private UUID userId;
    private String email;
    private String role;
}
