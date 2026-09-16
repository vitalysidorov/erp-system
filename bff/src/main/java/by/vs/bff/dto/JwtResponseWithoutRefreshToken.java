package by.vs.bff.dto;

import lombok.Getter;

@Getter
public class JwtResponseWithoutRefreshToken {
    private final String type = "Bearer";
    private final String accessToken;

    public JwtResponseWithoutRefreshToken (String accessToken) {
        this.accessToken = accessToken;
    }
}
