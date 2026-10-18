package com.adconnect.auth.controller;

import com.adconnect.auth.dto.AuthDtos.CreateManagerRequest;
import com.adconnect.auth.dto.AuthDtos.UpdateStatusRequest;
import com.adconnect.auth.dto.AuthDtos.UserDto;
import com.adconnect.auth.dto.AuthDtos.UserStats;
import com.adconnect.auth.entity.UserStatus;
import com.adconnect.auth.service.UserAdminService;
import com.adconnect.common.security.AuthUser;
import com.adconnect.common.security.CurrentUser;
import com.adconnect.common.security.Role;
import com.adconnect.common.web.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User administration (site managers)")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService service;

    @GetMapping
    public PageResponse<UserDto> list(@AuthUser CurrentUser actor,
                                      @RequestParam(required = false) Role role,
                                      @RequestParam(required = false) UserStatus status,
                                      @RequestParam(required = false) String q,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        actor.require(Role.MANAGER, Role.ADMIN);
        return service.search(role, status, q, page, size);
    }

    @GetMapping("/stats")
    public UserStats stats(@AuthUser CurrentUser actor) {
        actor.require(Role.MANAGER, Role.ADMIN);
        return service.stats();
    }

    @PatchMapping("/{id}/status")
    public UserDto updateStatus(@AuthUser CurrentUser actor, @PathVariable Long id,
                                @Valid @RequestBody UpdateStatusRequest request) {
        actor.require(Role.MANAGER, Role.ADMIN);
        return service.updateStatus(actor, id, request);
    }

    @PostMapping("/managers")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createManager(@AuthUser CurrentUser actor, @Valid @RequestBody CreateManagerRequest request) {
        actor.require(Role.ADMIN);
        return service.createManager(request);
    }
}
