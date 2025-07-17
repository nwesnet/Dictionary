package dictionarymicroservice.Configs;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/logout", "/register", "/css/**", "/js/**", "/api/**", "/oauth/callback", "/auth_success", "/auth_success.html").permitAll()
                        .anyRequest().authenticated()
                )
                .logout(logout -> logout.disable())
                .addFilterBefore(new dictionarymicroservice.Filters.JwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
