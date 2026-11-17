package com.eshopping.wallet.config;

import com.eshopping.wallet.security.JwtAuthenticationFilter;
import com.eshopping.wallet.security.RestAccessDeniedHandler;
import com.eshopping.wallet.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RestAuthenticationEntryPoint authenticationEntryPoint,
                          RestAccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .cors().and()
                .csrf().disable()
                .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .exceptionHandling()
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler)
                .and()
                .authorizeRequests()
                    // OpenAPI & Swagger Documentation
                    .antMatchers(
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/v3/api-docs/**",
                            "/swagger-resources/**"
                    ).permitAll()

                    // Actuator Health Probes
                    .antMatchers("/actuator/**").permitAll()

                    // Internal Saga endpoints validated via custom service headers in controller
                    .antMatchers(
                            "/api/wallet/pay",
                            "/api/wallet/payment",
                            "/api/wallet/refund",
                            "/api/wallet/payment/order/**"
                    ).permitAll()

                    // Customer wallet endpoints strictly require authentication
                    .antMatchers(
                            "/api/wallet",
                            "/api/wallet/balance",
                            "/api/wallet/transactions",
                            "/api/wallet/deposit"
                    ).authenticated()

                    .anyRequest().authenticated();

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }
}

