package com.example.ratelimit.filter;

import com.example.ratelimit.dto.RateLimitError;
import com.example.ratelimit.service.RateLimiterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
@Priority(Priorities.AUTHORIZATION)
public class RateLimitFilter implements ContainerRequestFilter {

    @Inject
    RateLimiterService rateLimiterService;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String clientId = resolveClientId(requestContext);
        if (clientId == null) {
            return;
        }

        if (rateLimiterService.isAdmin(clientId)) {
            return;
        }

        RateLimiterService.CheckResult result = rateLimiterService.checkRateLimit(clientId);
        if (!result.isAllowed()) {
            RateLimitError error = new RateLimitError(result.getReason());
            String body = mapper.writeValueAsString(error);
            requestContext.abortWith(
                Response.status(Response.Status.TOO_MANY_REQUESTS)
                    .entity(body)
                    .type(MediaType.APPLICATION_JSON)
                    .build()
            );
        }
    }

    private String resolveClientId(ContainerRequestContext ctx) {
        String clientId = ctx.getHeaderString("X-Client-ID");
        if (clientId != null && !clientId.isBlank()) {
            return clientId;
        }

        clientId = ctx.getUriInfo().getQueryParameters().getFirst("clientId");
        if (clientId != null && !clientId.isBlank()) {
            return clientId;
        }

        String remoteAddr = ctx.getHeaderString("X-Forwarded-For");
        if (remoteAddr != null && !remoteAddr.isBlank()) {
            return remoteAddr.split(",")[0].trim();
        }

        if (ctx.getUriInfo().getBaseUri().getHost() != null) {
            return ctx.getUriInfo().getBaseUri().getHost();
        }

        return null;
    }
}
