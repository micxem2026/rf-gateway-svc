package me.rightsflow.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContext {
    private String userId;
    private List<String> roles;
    private String userType;
    private String clientId;
    private List<String> scopes;
    private String subject;
}