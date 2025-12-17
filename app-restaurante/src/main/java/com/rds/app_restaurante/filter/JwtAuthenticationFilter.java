package com.rds.app_restaurante.filter;

import com.rds.app_restaurante.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtUtil jwtUtil;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        final String requestPath = request.getRequestURI();
        final String method = request.getMethod();
        
        // Permitir OPTIONS (preflight CORS) sin procesar
        if ("OPTIONS".equals(method)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Rutas públicas que no requieren autenticación - omitir filtro JWT completamente
        if (requestPath.startsWith("/api/public/") || 
            requestPath.startsWith("/api/auth/") ||
            requestPath.startsWith("/actuator/") ||
            requestPath.startsWith("/v3/api-docs/") ||
            requestPath.startsWith("/swagger-ui") ||
            requestPath.equals("/") ||
            requestPath.equals("/api") ||
            requestPath.startsWith("/error")) {
            // No procesar estas rutas con JWT, pasar directamente
            filterChain.doFilter(request, response);
            return;
        }
        
        // Para todas las demás rutas, verificar el token JWT
        final String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // No hay token, dejar que Spring Security maneje la autenticación
            // No loguear aquí porque Spring Security ya lo manejará y devolverá 401/403
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            final String token = authHeader.substring(7);
            System.out.println("JWT Filter: Processing token for: " + request.getRequestURI());
            
            final String email = jwtUtil.extractEmail(token);
            System.out.println("JWT Filter: Email extracted: " + email);
            
            if (email != null) {
                // Siempre establecer el SecurityContext si tenemos un email válido, incluso si ya está establecido
                // (esto asegura que se actualice si hay un token nuevo)
                if (!jwtUtil.isTokenExpired(token)) {
                    com.rds.app_restaurante.model.Role role = jwtUtil.extractRole(token);
                    System.out.println("JWT Filter: Role extracted: " + role.name());
                    
                    String authority = "ROLE_" + role.name();
                    System.out.println("JWT Filter: Setting authority: " + authority);
                    
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority(authority))
                    );
                    
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    System.out.println("JWT Authentication successful for: " + email + " with role: " + authority + " for URI: " + request.getRequestURI());
                    System.out.println("JWT Filter: SecurityContext authentication set: " + 
                        (SecurityContextHolder.getContext().getAuthentication() != null));
                    System.out.println("JWT Filter: SecurityContext authorities: " + 
                        SecurityContextHolder.getContext().getAuthentication().getAuthorities());
                } else {
                    System.err.println("JWT Token expired for: " + email + " for URI: " + request.getRequestURI());
                    // Limpiar SecurityContext si el token está expirado
                    SecurityContextHolder.clearContext();
                }
            } else {
                System.err.println("JWT Filter: Could not extract email from token for: " + request.getRequestURI());
                // Limpiar SecurityContext si no podemos extraer el email
                SecurityContextHolder.clearContext();
            }
        } catch (Exception e) {
            // Token inválido o expirado, limpiar contexto y continuar
            System.err.println("JWT Authentication error for: " + request.getRequestURI() + " - " + e.getMessage());
            System.err.println("JWT Filter: Exception type: " + e.getClass().getSimpleName());
            e.printStackTrace();
            SecurityContextHolder.clearContext();
        }
        
        filterChain.doFilter(request, response);
    }
}

