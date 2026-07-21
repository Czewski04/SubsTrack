package org.wilczewski.substrack.auth.internal;

import jdk.jfr.Registered;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.wilczewski.substrack.auth.api.dto.command.LoginCommand;
import org.wilczewski.substrack.auth.api.dto.command.RegisterCommand;
import org.wilczewski.substrack.auth.api.dto.request.LoginRequest;
import org.wilczewski.substrack.auth.api.dto.request.RegisterRequest;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuthMapper {
    RegisterCommand toRegisterCommand(RegisterRequest request);
    LoginCommand toLoginCommand(LoginRequest request);
}
