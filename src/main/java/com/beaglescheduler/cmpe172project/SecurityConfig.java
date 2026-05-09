package com.beaglescheduler.cmpe172project;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // -------------------------------------------------------------------------
    // FILTER CHAIN 1 — Authorization Server (issues tokens)
    // -------------------------------------------------------------------------
    @Bean
    @Order(1)
    public SecurityFilterChain authServerFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer authServer = new OAuth2AuthorizationServerConfigurer();

        http
            .securityMatcher(authServer.getEndpointsMatcher())
            .with(authServer, Customizer.withDefaults())
            .cors(Customizer.withDefaults());

        return http.build();
    }

    // -------------------------------------------------------------------------
    // FILTER CHAIN 2 — REST API (JWT-protected, only /add)
    // -------------------------------------------------------------------------
    @Bean
    @Order(2)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/add")
            .authorizeHttpRequests(a -> a.anyRequest().authenticated())
            .oauth2ResourceServer(r -> r.jwt(Customizer.withDefaults()))
            .cors(Customizer.withDefaults());

        return http.build();
    }

    // -------------------------------------------------------------------------
    // FILTER CHAIN 3 — Web UI catch-all (Thymeleaf + form login)
    // No securityMatcher: handles every request not claimed by chains 1 or 2.
    // -------------------------------------------------------------------------
    @Bean
    @Order(3)
    public SecurityFilterChain webUiFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(a -> a
                .requestMatchers("/", "/slots", "/book", "/confirmation", "/health",
                                 "/css/**", "/h2-console/**", "/mock/notify",
                                 "/appointments/*/notify").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/technician/**").hasRole("TECHNICIAN")
                .anyRequest().authenticated()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**", "/mock/notify", "/appointments/*/notify")
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.disable())
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", false)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedHandler((request, response, e) ->
                    response.sendRedirect("/login?denied"))
            );

        return http.build();
    }

    // -------------------------------------------------------------------------
    // JDBC-backed UserDetailsService for form login
    // -------------------------------------------------------------------------
    @Bean
    public UserDetailsService userDetailsService(JdbcTemplate jdbc) {
        return username -> {
            List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT email, password, role FROM users WHERE email = ?", username);
            if (rows.isEmpty()) {
                throw new UsernameNotFoundException("User not found: " + username);
            }
            Map<String, Object> row = rows.get(0);
            String pwd = row.get("password") != null
                ? (String) row.get("password")
                : "{noop}" + UUID.randomUUID();
            String role = (String) row.get("role");
            return User.withUsername(username)
                .password(pwd)
                .roles(role)
                .build();
        };
    }

    // -------------------------------------------------------------------------
    // Password Encoder
    // -------------------------------------------------------------------------
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    // -------------------------------------------------------------------------
    // CORS Configuration
    // -------------------------------------------------------------------------
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("*"));
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // -------------------------------------------------------------------------
    // RestTemplate (used by notification controllers for loopback calls)
    // -------------------------------------------------------------------------
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    // -------------------------------------------------------------------------
    // Registered OAuth2 Client
    // -------------------------------------------------------------------------
    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        return new InMemoryRegisteredClientRepository(
            RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("client")
                .clientSecret("{noop}secret")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("read")
                .build()
        );
    }

    // -------------------------------------------------------------------------
    // RSA Key Pair (sign/verify JWTs)
    // -------------------------------------------------------------------------
    @Bean
    public JWKSource<SecurityContext> jwkSource() throws Exception {
        RSAKey key = new RSAKeyGenerator(2048).keyID("1").generate();
        return new ImmutableJWKSet<>(new JWKSet(key));
    }

    // -------------------------------------------------------------------------
    // JWT Decoder
    // -------------------------------------------------------------------------
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        var keySelector = new JWSVerificationKeySelector<>(Set.of(JWSAlgorithm.RS256), jwkSource);
        var jwtProcessor = new DefaultJWTProcessor<>();
        jwtProcessor.setJWSKeySelector(keySelector);
        return new NimbusJwtDecoder(jwtProcessor);
    }
}
