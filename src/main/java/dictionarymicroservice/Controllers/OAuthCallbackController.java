package dictionarymicroservice.Controllers;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
public class OAuthCallbackController {
    @GetMapping("/oauth/callback")
    public void handleCallback(@RequestParam String code, HttpServletResponse response) throws IOException {
        RestTemplate restTemplate = new RestTemplate();
        LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);

        ResponseEntity<Map> jwtResponse = restTemplate.postForEntity(
                "http://localhost:8080/oauth/token",
                body,
                Map.class
        );

        Map<String, Object> jwtBody = jwtResponse.getBody();
        if (jwtBody == null || jwtBody.get("token") == null) {
            response.sendRedirect("/login?error=NoTokenReceived");
            return;
        }

        String token = (String) jwtResponse.getBody().get("token");

        Cookie cookie = new Cookie("JWT_TOKEN", token);
        // The second way to implement the cross application authorization
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        response.sendRedirect("/");
        // The Authorization header way
//        response.sendRedirect("/auth_success.html#token=" + token);
    }
}
