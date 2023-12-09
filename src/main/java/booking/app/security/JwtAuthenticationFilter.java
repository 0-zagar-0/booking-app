package booking.app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER_NAME = "Bearer ";
    private static final int TOKEN_START_INDEX = 7;

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {
        String bearerToken = getToken(request);

        if (bearerToken != null && jwtUtil.isValidToken(bearerToken)) {
            String userName = jwtUtil.getUserName(bearerToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(userName);
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    userDetails.isCredentialsNonExpired(),
                    userDetails.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    private String getToken(final HttpServletRequest request) {
        final String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_NAME)) {
            return bearerToken.substring(TOKEN_START_INDEX);
        }

        return null;
    }
}
