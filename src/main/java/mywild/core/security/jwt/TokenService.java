package mywild.core.security.jwt;

import java.security.interfaces.RSAPrivateKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.extern.slf4j.Slf4j;
import mywild.user.UserEntity;

@Slf4j
@Service
public class TokenService {

    @Value("${mywild.jwt.issuer}")
    private String issuer;

    @Value("${mywild.jwt.subject}")
    private String subject;

    @Value("${mywild.jwt.audience}")
    private String audience;

    @Value("${mywild.jwt.access-token-duration}")
    private int accessTokenDuration;

    @Value("${mywild.jwt.refresh-token-duration}")
    private int refreshTokenDuration;

    @Autowired
    private RSAPrivateKey privateKey;

    public String generateToken(TokenType tokenType, UserEntity user) {
        try {
            JWSHeader jwtHeader = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("k1").build();
            JWTClaimsSet jwtClaims = new JWTClaimsSet.Builder()
                    .jwtID(Long.toString(System.currentTimeMillis()))
                    .issuer(issuer)
                    .subject(subject)
                    .audience(audience)
                    .issueTime(new Date())
                    .expirationTime(tokenType == TokenType.ACCESS
                        ? Date.from(LocalDateTime.now().plusMinutes(accessTokenDuration).atZone(ZoneId.systemDefault()).toInstant()) // Access
                        : Date.from(LocalDateTime.now().plusHours(refreshTokenDuration).atZone(ZoneId.systemDefault()).toInstant()) // Refresh
                    )
                    .claim("scope", tokenType.toString().toLowerCase())
                    .claim(TokenConstants.JWT_USER_ID, user.getId())
                    .claim(TokenConstants.JWT_USER_WILDEVENTS, user.getUsername())
                    .claim(TokenConstants.JWT_USER_INATURALIST, user.getInaturalist())
                    .build();
            SignedJWT jws = new SignedJWT(jwtHeader, jwtClaims);
            jws.sign(new RSASSASigner(privateKey));
            // TODO: Encrypt the JWS (signed JWT) token to get a JWE token
            // JWEHeader jweHeader = new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A256GCM).contentType("JWT").build();
            // JWEObject jwe = new JWEObject(jweHeader, new Payload(jws));
            // jwe.encrypt(new RSAEncrypter(publicKey));
            // String token = jwe.serialize();
            return jws.serialize();
        }
        catch (JOSEException ex) {
            log.error(ex.toString());
        }
        return null;
    }

}
