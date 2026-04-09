package com.edupulse.config;

import com.edupulse.model.Faculty;
import com.edupulse.repository.FacultyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private FacultyRepository facultyRepo;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
            .authorizeHttpRequests(auth -> auth
                // ✅ PUBLIC ENDPOINTS
                .requestMatchers(
                        "/", 
                        "/login", 
                        "/feedback/**",   // 🔥 IMPORTANT FIX
                        "/css/**", 
                        "/js/**", 
                        "/images/**", 
                        "/error"
                ).permitAll()

                // Principal only
                .requestMatchers("/principal/**").hasRole("PRINCIPAL")

                // Staff only
                .requestMatchers("/staff/**").hasRole("STAFF")

                .anyRequest().authenticated()
            )

            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(customAuthenticationSuccessHandler())
                .failureHandler(customAuthenticationFailureHandler())
                .permitAll()
            )

            .exceptionHandling(ex -> ex
                .accessDeniedHandler((request, response, denied) -> {
                    String referer = request.getHeader("Referer");
                    if (referer != null && referer.contains("/principal/")) {
                        response.sendRedirect("/login?type=principal&error=unauthorized");
                    } else if (referer != null && referer.contains("/staff/")) {
                        response.sendRedirect("/login?type=staff&error=unauthorized");
                    } else {
                        response.sendRedirect("/login?error=unauthorized");
                    }
                })
            )

            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }

    // KEEP REST SAME (no change)
    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return new SimpleUrlAuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, 
                                                HttpServletResponse response,
                                                Authentication authentication) 
                                                throws IOException, ServletException {

                String portalType = request.getParameter("portalType");
                String username = request.getParameter("username");

                Faculty faculty = facultyRepo.findByUsername(username).orElse(null);

                if (faculty != null) {
                    String role = faculty.getRole().name();

                    if ("principal".equals(portalType) && "PRINCIPAL".equals(role)) {
                        response.sendRedirect("/principal/dashboard");
                        return;
                    } else if ("staff".equals(portalType) && "STAFF".equals(role)) {
                        response.sendRedirect("/staff/dashboard");
                        return;
                    } else if ("principal".equals(portalType) && "STAFF".equals(role)) {
                        response.sendRedirect("/login?type=principal&error=invalidrole");
                        return;
                    } else if ("staff".equals(portalType) && "PRINCIPAL".equals(role)) {
                        response.sendRedirect("/login?type=staff&error=invalidrole");
                        return;
                    }
                }

                response.sendRedirect("/login?error=true");
            }
        };
    }

    @Bean
    public AuthenticationFailureHandler customAuthenticationFailureHandler() {
        return new SimpleUrlAuthenticationFailureHandler() {
            @Override
            public void onAuthenticationFailure(HttpServletRequest request, 
                                               HttpServletResponse response,
                                               AuthenticationException exception) 
                                               throws IOException, ServletException {

                String portalType = request.getParameter("portalType");
                String username = request.getParameter("username");

                if (username != null && !username.isEmpty()) {
                    Faculty faculty = facultyRepo.findByUsername(username).orElse(null);

                    if (faculty != null) {
                        String role = faculty.getRole().name();

                        if ("principal".equals(portalType) && "STAFF".equals(role)) {
                            response.sendRedirect("/login?type=principal&error=invalidrole");
                            return;
                        } else if ("staff".equals(portalType) && "PRINCIPAL".equals(role)) {
                            response.sendRedirect("/login?type=staff&error=invalidrole");
                            return;
                        }
                    }
                }

                if ("principal".equals(portalType)) {
                    response.sendRedirect("/login?type=principal&error=invalidcredentials");
                } else if ("staff".equals(portalType)) {
                    response.sendRedirect("/login?type=staff&error=invalidcredentials");
                } else {
                    response.sendRedirect("/login?error=true");
                }
            }
        };
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            Faculty faculty = facultyRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

            String role = "ROLE_" + faculty.getRole().name();

            return new User(
                faculty.getUsername(), 
                faculty.getPassword(),
                List.of(new SimpleGrantedAuthority(role))
            );
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}