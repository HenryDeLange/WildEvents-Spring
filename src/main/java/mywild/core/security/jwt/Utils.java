package mywild.core.security.jwt;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class Utils {

    private Utils() {
    }

    public static String getUserIdFromJwt(JwtAuthenticationToken jwtToken) {
        return jwtToken.getTokenAttributes().get(TokenConstants.JWT_USER_ID).toString();
    }

    public static String getUsernameFromJwt(JwtAuthenticationToken jwtToken) {
        return jwtToken.getTokenAttributes().get(TokenConstants.JWT_USER_WILDEVENTS).toString();
    }

    public static String getINatNameFromJwt(JwtAuthenticationToken jwtToken) {
        return jwtToken.getTokenAttributes().get(TokenConstants.JWT_USER_INATURALIST).toString();
    }

}
