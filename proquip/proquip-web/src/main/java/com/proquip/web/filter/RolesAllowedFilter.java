package com.proquip.web.filter;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.lang.reflect.Method;

@Provider
public class RolesAllowedFilter implements DynamicFeature {

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        Method method = resourceInfo.getResourceMethod();
        Class<?> clazz = resourceInfo.getResourceClass();

        RolesAllowed methodRoles = method.getAnnotation(RolesAllowed.class);
        if (methodRoles != null) {
            context.register(new RolesCheckFilter(methodRoles.value()));
            return;
        }

        if (method.isAnnotationPresent(PermitAll.class) || method.isAnnotationPresent(DenyAll.class)) {
            return;
        }

        RolesAllowed classRoles = clazz.getAnnotation(RolesAllowed.class);
        if (classRoles != null) {
            context.register(new RolesCheckFilter(classRoles.value()));
        }
    }

    private static class RolesCheckFilter implements ContainerRequestFilter {
        private final String[] allowedRoles;

        RolesCheckFilter(String[] allowedRoles) {
            this.allowedRoles = allowedRoles;
        }

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            SecurityContext sc = requestContext.getSecurityContext();
            if (sc == null || sc.getUserPrincipal() == null) {
                requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
                        .entity("{\"error\":\"アクセス権限がありません。\"}")
                        .build());
                return;
            }

            for (String role : allowedRoles) {
                if (sc.isUserInRole(role)) {
                    return;
                }
            }

            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"error\":\"この操作には管理者権限が必要です。\"}")
                    .build());
        }
    }
}
