package vn.luxuryhotel.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import vn.luxuryhotel.repository.UserRepository;

@Configuration @RequiredArgsConstructor
public class SecurityConfig {
    private final UserRepository users;
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}
    @Bean UserDetailsService userDetailsService(){return email->{
        var u=users.findByEmail(email.trim().toLowerCase(Locale.ROOT)).orElseThrow(()->new UsernameNotFoundException("Thông tin đăng nhập không đúng."));
        return User.withUsername(u.getEmail()).password(u.getPassword()).roles(u.getRole().getName()).disabled(!u.isEnabled()).build();
    };}
    @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth->auth
            .requestMatchers("/","/rooms","/rooms/*","/login","/register","/css/**","/js/**","/images/**","/vendor/**","/error","/error/**").permitAll()
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .requestMatchers("/staff/**").hasAnyRole("ADMIN","STAFF")
            .anyRequest().authenticated())
            .formLogin(form->form.loginPage("/login").defaultSuccessUrl("/account",true).failureUrl("/login?error").permitAll())
            .logout(logout->logout.logoutSuccessUrl("/login?logout"))
            .exceptionHandling(e->e.accessDeniedPage("/error/403"));
        // Refresh database role/enabled state on every request, including existing sessions.
        http.addFilterBefore(new OncePerRequestFilter(){
            @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
                var auth=SecurityContextHolder.getContext().getAuthentication();
                if(auth!=null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                    var u=users.findByEmail(auth.getName()).orElse(null);
                    if(u==null||!u.isEnabled()) {
                        SecurityContextHolder.clearContext();
                        var session=request.getSession(false);if(session!=null)session.invalidate();
                        response.sendRedirect(request.getContextPath()+"/login?locked");return;
                    }
                    var updated=new UsernamePasswordAuthenticationToken(auth.getPrincipal(),null,List.of(new SimpleGrantedAuthority("ROLE_"+u.getRole().getName())));
                    SecurityContextHolder.getContext().setAuthentication(updated);
                }
                chain.doFilter(request,response);
            }
        },AuthorizationFilter.class);
        return http.build();
    }
}
