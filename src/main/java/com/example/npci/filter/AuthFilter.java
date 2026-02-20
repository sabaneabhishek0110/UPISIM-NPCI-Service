package com.example.npci.filter;

import com.example.npci.service.NpciService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.List;

public class AuthFilter extends OncePerRequestFilter {

    private final NpciService npciService;

    public AuthFilter(NpciService npciService) {
        this.npciService = npciService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException{

        CachedBodyRequestWrapper wrappedRequest =
                new CachedBodyRequestWrapper(request);

        String path = wrappedRequest.getRequestURI();

        System.out.println("Entered Into filter : "+path);

        if (path.startsWith("/health") ||
                path.startsWith("/internal/npci/psp/register-public-key") ||
                path.startsWith("/internal/npci/icici/register-public-key") ||
                path.startsWith("/internal/npci/hdfc/register-public-key")){

            filterChain.doFilter(wrappedRequest, response);
            System.out.println("Passed through filter : "+path);
            return;
        }


        String base64Signature = wrappedRequest.getHeader("X-SIGNATURE");
        String pspId = wrappedRequest.getHeader("X-PSP-CODE");
        if(pspId==null || base64Signature==null){
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        wrappedRequest.getInputStream().readAllBytes();

        String payload = new String(
                wrappedRequest.getCachedBody(),
                StandardCharsets.UTF_8
        );

        String public_key = npciService.loadPublicKeyFromdb(pspId);
        PublicKey publicKey = npciService.convertToPublicKey(public_key);
        boolean valid = npciService.verify(payload,base64Signature,publicKey);

        if(!valid){
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        pspId,        // principal (PSP identity)
                        null,         // no credentials
                        List.of()     // no roles (or add ROLE_PSP)
                );

        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(wrappedRequest, response);
        System.out.println("Completed Filter : "+path);
    }

}
