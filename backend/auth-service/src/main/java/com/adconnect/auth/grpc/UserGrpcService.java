package com.adconnect.auth.grpc;

import com.adconnect.auth.mapper.UserMapper;
import com.adconnect.auth.repository.UserRepository;
import com.adconnect.grpc.user.GetUserRequest;
import com.adconnect.grpc.user.GetUsersRequest;
import com.adconnect.grpc.user.UserReply;
import com.adconnect.grpc.user.UserServiceGrpc;
import com.adconnect.grpc.user.UsersReply;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;

@GrpcService
@RequiredArgsConstructor
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final UserRepository users;
    private final UserMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public void getUser(GetUserRequest request, StreamObserver<UserReply> responseObserver) {
        users.findById(request.getId()).ifPresentOrElse(user -> {
            responseObserver.onNext(mapper.toReply(user));
            responseObserver.onCompleted();
        }, () -> responseObserver.onError(Status.NOT_FOUND
                .withDescription("User " + request.getId() + " not found").asRuntimeException()));
    }

    @Override
    @Transactional(readOnly = true)
    public void getUsers(GetUsersRequest request, StreamObserver<UsersReply> responseObserver) {
        UsersReply.Builder reply = UsersReply.newBuilder();
        users.findAllById(request.getIdsList()).forEach(u -> reply.addUsers(mapper.toReply(u)));
        responseObserver.onNext(reply.build());
        responseObserver.onCompleted();
    }
}
