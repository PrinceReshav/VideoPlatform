package com.platform.common.config;

import com.platform.common.security.PlatformUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Base64;
import java.util.UUID;

@Configuration
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class CommonSecurityConfiguration {

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    SecurityFilterChain platformSecurityFilterChain(HttpSecurity http, PlatformJwtFilter jwtFilter) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/videos/**", "/api/v1/channels/**", "/api/v1/categories/**", "/api/v1/genres/**", "/api/v1/languages/**", "/api/v1/search/**", "/api/v1/live/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/search/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @ConditionalOnMissingBean
    PlatformJwtFilter platformJwtFilter(@Value("${jwt.secret:dev_secret_key_change_in_production_min_256_bits_abcdefghijklmnopqrstuvwx}") String secret) {
        return new PlatformJwtFilter(secret);
    }

    static final class PlatformJwtFilter extends OncePerRequestFilter {
        private final SecretKey key;

        PlatformJwtFilter(String secret) {
            this.key = createKey(secret);
        }

        private static SecretKey createKey(String secret) {
            try {
                byte[] decoded = Base64.getDecoder().decode(secret);
                if (decoded.length >= 32) return Keys.hmacShaKeyFor(decoded);
            } catch (IllegalArgumentException ignored) { }
            return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
                                        jakarta.servlet.http.HttpServletResponse response,
                                        jakarta.servlet.FilterChain filterChain) throws java.io.IOException, jakarta.servlet.ServletException {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                try {
                    Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(header.substring(7)).getPayload();
                    if ("access".equals(claims.get("type"))) {
                        UUID userId = UUID.fromString(claims.getSubject());
                        String username = claims.get("username", String.class);
                        String email = claims.get("email", String.class);
                        List<PlatformUserDetails.Role> roles = rolesFrom(claims.get("roles"));
                        PlatformUserDetails details = new PlatformUserDetails(userId, email, username, null, roles, true, true, true, true);
                        SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
                    }
                } catch (Exception ignored) {
                    SecurityContextHolder.clearContext();
                }
            }
            filterChain.doFilter(request, response);
        }

        private List<PlatformUserDetails.Role> rolesFrom(Object raw) {
            List<PlatformUserDetails.Role> roles = new ArrayList<>();
            if (raw instanceof Collection<?> values) {
                for (Object value : values) {
                    try { roles.add(PlatformUserDetails.Role.valueOf(String.valueOf(value))); } catch (Exception ignored) { }
                }
            }
            return roles;
        }
    }
}
