package com.openmarket.controller;

import com.openmarket.dto.Role;
import com.openmarket.dto.user.CreateUserRequest;
import com.openmarket.dto.user.UpdatePasswordRequest;
import com.openmarket.dto.user.UserResponse;
import com.openmarket.security.UserPrincipal;
import com.openmarket.service.UserService;
import com.openmarket.utils.LogWrapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        var user = LogWrapper.logWrap(
                log,
                "createUser",
                () -> userService.createUser(request, principal.getUserId())
        );
        return ResponseEntity.ok(user);
    }

    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> getUsers(@PageableDefault(size = 20) Pageable pageable,
                                                       @RequestParam(required = false) Role role) {
        var users = LogWrapper.logWrap(
                log,
                "getUsers",
                () -> Objects.nonNull(role) ? userService.getUsersByRole(role, pageable) : userService.getAllUsers(pageable)
        );
        return ResponseEntity.ok(users);
    }

    @PutMapping("/users/{userId}/password")
    public ResponseEntity<UserResponse> updateUserPassword(@PathVariable UUID userId,
                                                           @Valid @RequestBody UpdatePasswordRequest request,
                                                           @AuthenticationPrincipal UserPrincipal principal) {
        var user = LogWrapper.logWrap(
                log,
                "updateUserPassword",
                () -> userService.updateUserPassword(userId, request.getNewPassword(), principal.getUserId())
        );
        return ResponseEntity.ok(user);
    }
}
