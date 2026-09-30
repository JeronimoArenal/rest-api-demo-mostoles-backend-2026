package com.example.spring_security_jwt.payload.request;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class LoginRequest {

    private String username;

    private String password;
}
