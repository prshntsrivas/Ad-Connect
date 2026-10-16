package com.adconnect.chat.client;

import com.adconnect.common.web.ApiException;
import com.adconnect.grpc.user.GetUserRequest;
import com.adconnect.grpc.user.UserReply;
import com.adconnect.grpc.user.UserServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class UserNameClient {

    @GrpcClient("auth-service")
    private UserServiceGrpc.UserServiceBlockingStub stub;

    public UserReply get(Long userId) {
        try {
            return stub.getUser(GetUserRequest.newBuilder().setId(userId).build());
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                throw ApiException.notFound("User not found");
            }
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "User directory unavailable");
        }
    }
}
