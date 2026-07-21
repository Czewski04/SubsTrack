package org.wilczewski.substrack.user.internal;

import org.mapstruct.*;
import org.wilczewski.substrack.user.api.dto.command.*;
import org.wilczewski.substrack.user.api.dto.response.UserCredentialsResponse;
import org.wilczewski.substrack.user.api.dto.response.UserEmailResponse;
import org.wilczewski.substrack.user.api.dto.response.UserResponse;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface UserMapper {
    @Mapping(target = "id", ignore = true)
    User toUser(CreateUserCommand command);
    @AfterMapping
    default void afterCreate(@MappingTarget User user) {
        user.addAdditionalEmail(user.getEmail());
    }

    UserResponse toUserResponse(User user);
    UserEmailResponse toUserEmailResponse(UserEmail userEmail);
    UserCredentialsResponse toUserCredentialsResponse(User user);
    List<UserResponse> toUserResponseList(List<User> users);
}
