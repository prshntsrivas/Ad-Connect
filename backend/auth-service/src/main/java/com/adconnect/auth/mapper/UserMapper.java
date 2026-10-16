package com.adconnect.auth.mapper;

import com.adconnect.auth.dto.AuthDtos.UserDto;
import com.adconnect.auth.entity.User;
import com.adconnect.grpc.user.UserReply;
import org.mapstruct.Mapper;

@Mapper
public interface UserMapper {

    UserDto toDto(User user);

    default UserReply toReply(User user) {
        return UserReply.newBuilder()
                .setId(user.getId())
                .setEmail(user.getEmail())
                .setFullName(user.getFullName())
                .setRole(user.getRole().name())
                .setStatus(user.getStatus().name())
                .setPhone(user.getPhone() == null ? "" : user.getPhone())
                .build();
    }
}
