package com.adconnect.campaign.client;

import com.adconnect.common.web.ApiException;
import com.adconnect.grpc.user.GetUserRequest;
import com.adconnect.grpc.user.GetUsersRequest;
import com.adconnect.grpc.user.UserReply;
import com.adconnect.grpc.user.UserServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Resolves account details from auth-service over gRPC (service located through Eureka). */
@Slf4j
@Component
public class UserDirectoryClient {

    @GrpcClient("auth-service")
    private UserServiceGrpc.UserServiceBlockingStub stub;

    /** Only verified accounts are cached, so a newly verified user is recognised immediately. */
    @Cacheable(cacheNames = "users", key = "#id", unless = "#result == null || !#result.isVerified()")
    public UserInfo getUser(Long id) {
        try {
            return toInfo(stub.getUser(GetUserRequest.newBuilder().setId(id).build()));
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                throw ApiException.notFound("User " + id + " not found");
            }
            log.error("auth-service gRPC call failed: {}", e.getStatus());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "User directory unavailable");
        }
    }

    public Map<Long, UserInfo> getUsers(Collection<Long> ids) {
        try {
            return stub.getUsers(GetUsersRequest.newBuilder().addAllIds(ids).build()).getUsersList().stream()
                    .map(UserDirectoryClient::toInfo)
                    .collect(Collectors.toMap(UserInfo::id, Function.identity()));
        } catch (StatusRuntimeException e) {
            log.warn("auth-service gRPC batch lookup failed: {}", e.getStatus());
            return Map.of();
        }
    }

    public UserInfo requireVerified(Long id) {
        UserInfo user = getUser(id);
        if (!user.isVerified()) {
            throw ApiException.forbidden("Your account is awaiting verification by a site manager");
        }
        return user;
    }

    private static UserInfo toInfo(UserReply reply) {
        return new UserInfo(reply.getId(), reply.getEmail(), reply.getFullName(), reply.getRole(), reply.getStatus());
    }
}
