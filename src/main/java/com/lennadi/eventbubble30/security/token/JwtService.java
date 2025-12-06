package com.lennadi.eventbubble30.security.token;

import com.lennadi.eventbubble30.features.service.BenutzerService;
import com.lennadi.eventbubble30.security.BenutzerDetails;
import com.lennadi.eventbubble30.security.BenutzerDetailsService;
import com.lennadi.eventbubble30.security.token.exceptions.*;
import com.lennadi.eventbubble30.config.ServerConfigService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JwtService {

    @Value("${app.jwt.secret:}")
    private String secret;

    @Value("${app.jwt.access-token-validity-ms:900000}")
    private long accessTokenValidityMs;

    @Value("${app.jwt.refresh-token-validity-ms:2592000000}")
    private long refreshTokenValidityMs;

    private final ServerConfigService serverConfigService;
    private final BenutzerDetailsService benutzerDetailsService;

    @PostConstruct
    public void init() {
        if(secret==null||secret.isBlank()) {
            System.err.println("WARNING: No JWT secret set!");
        }
    }

    /// //////////////////////////////////////////////
    public String generateAccessToken(BenutzerDetails user){
        return buildToken(user, accessTokenValidityMs, TokenType.ACCESS);
    }

    public String generateRefreshToken(BenutzerDetails user){
        return buildToken(user, refreshTokenValidityMs, TokenType.REFRESH);
    }
    /// /////////////////////////////////////////////////
    public BenutzerDetails validateAccessToken(String token){
        return validateTokenInternal(token, TokenType.ACCESS);
    }

    public BenutzerDetails validateRefreshToken(String token){
        return validateTokenInternal(token, TokenType.REFRESH);
    }
    /// /////////////////////////////////////////////////

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private String buildToken(BenutzerDetails user, long validity, TokenType type) {
        Instant now = Instant.now();
        Date issuedAt = Date.from(now);
        Date expiryAt = new Date(issuedAt.getTime() + validity);

        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .setIssuedAt(issuedAt)
                .setExpiration(expiryAt)
                .addClaims(Map.of(
                        "username", user.getUsername(),//remove?
                        "roles", roles,
                        "type", type.name()
                ))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims parseAllClaims(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


    private BenutzerDetails validateTokenInternal(String token, TokenType expectedType) {
        Claims claims;
        try{
            claims = parseAllClaims(token);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new TokenExpiredException();
        } catch(SignatureException se){
            throw new TokenBadSignatureException();
        } catch (JwtException e) {
            throw new MalformedOrMissingTokenException();
        }

        String typeStr = claims.get("type", String.class);
        Date issuedAtDate = claims.getIssuedAt();

        /// TokenType valide
        TokenType type;
        try{
            type = TokenType.valueOf(typeStr);
        }catch (Exception e){
            throw new MalformedOrMissingTokenException();
        }

        /// User Id check
        long tokenUserId;
        try{
            tokenUserId = Long.parseLong(claims.getSubject());
        }catch (Exception e){
            throw new MalformedOrMissingTokenException();
        }

        BenutzerDetails user;
        try{
            user = benutzerDetailsService.loadUserById(tokenUserId);
        }catch (Exception e){
            throw new TokenUserDoesNotExistException(tokenUserId);
        }

        /// Typ Check
        if(type!=expectedType){
            throw new WrongTokenTypeException(expectedType, type);
        }

        /*
        wird bereits beim Claims parsen gehandhabt
        /// Ablaufdatum Check
        if(isTokenExpired(claims)){
            throw new TokenExpiredException();
        }
        */

        /// issuedAt existiert
        if(issuedAtDate == null)
            throw new MalformedOrMissingTokenException();
        Instant issuedAt = issuedAtDate.toInstant();

        /// ////////////////////////////////////////////////////////////////////////////////////invalidiert?
        /// Token Global invalidiert?
        {
            Instant globalInvalidatedAt = serverConfigService.getGlobalTokenRevokationTime();
            if (issuedAt.isBefore(globalInvalidatedAt)) {
                throw new GlobalTokenRevokedException(globalInvalidatedAt);
            }
        }

        /// Per Nutzer Revoked?
        {
            Instant uRAt = user.getTokensInvalidatedAt();
            if(issuedAt.isBefore(uRAt)){
                throw new UserTokenRevokedException(uRAt);
            }
        }

        /// Passwort geändert?
        {
            Instant pwUAt = user.getPasswordChangedAt();
            if(issuedAt.isBefore(pwUAt)){
                throw new PasswordChangedTokenRevokedException(pwUAt);
            }
        }

        return user;
    }
}
