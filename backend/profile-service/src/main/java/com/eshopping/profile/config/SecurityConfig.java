package com.eshopping.profile.config;

import com.eshopping.profile.security.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AntiCsrfFilter antiCsrfFilter;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          AntiCsrfFilter antiCsrfFilter,
                          OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
                          RestAuthenticationEntryPoint authenticationEntryPoint,
                          RestAccessDeniedHandler accessDeniedHandler) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.antiCsrfFilter = antiCsrfFilter;
        this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .cors().and()
                .csrf().disable() // Stateless JWT session management; cookie endpoints guarded by AntiCsrfFilter
                .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .exceptionHandling()
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler)
                .and()
                .authorizeRequests()
                    // Public Authentication Endpoints
                    .antMatchers(
                            "/api/auth/register",
                            "/api/auth/register-merchant",
                            "/api/auth/login",
                            "/api/auth/refresh",
                            "/api/auth/oauth2/exchange",
                            "/error"
                    ).permitAll()
                    // OpenAPI & Swagger Documentation
                    .antMatchers(
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/v3/api-docs/**",
                            "/swagger-resources/**"
                    ).permitAll()
                    // Actuator Health Probes
                    .antMatchers("/actuator/**").permitAll()
                    // OAuth2 Flow Endpoints
                    .antMatchers("/oauth2/**", "/login/**", "/login/oauth2/**").permitAll()
                    // All other APIs require authenticated bearer token
                    .anyRequest().authenticated()
                .and()
                .oauth2Login()
                    .successHandler(oAuth2AuthenticationSuccessHandler)
                    .failureHandler((req, res, ex) -> {
                        log.error("OAuth2 authentication failure: {}", ex.getMessage());
                        res.sendRedirect("http://localhost:5173/login?error=" + java.net.URLEncoder.encode(ex.getMessage(), "UTF-8"));
                    });

        // Security Filters Registration
        http.addFilterBefore(antiCsrfFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }
}

