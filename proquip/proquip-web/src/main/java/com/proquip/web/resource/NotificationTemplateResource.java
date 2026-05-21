package com.proquip.web.resource;

import com.proquip.ejb.entity.system.NotificationTemplate;

import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("/notification-templates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMIN"})
public class NotificationTemplateResource {

    @PersistenceContext
    private EntityManager em;

    @GET
    public Response listTemplates() {
        List<NotificationTemplate> list = em.createQuery(
                "SELECT t FROM NotificationTemplate t ORDER BY t.eventType, t.name", NotificationTemplate.class)
                .getResultList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (NotificationTemplate t : list) {
            result.add(toMap(t));
        }
        return Response.ok(result).build();
    }

    @GET
    @Path("/{id}")
    public Response getTemplate(@PathParam("id") Long id) {
        NotificationTemplate t = em.find(NotificationTemplate.class, id);
        if (t == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(toMap(t)).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Response updateTemplate(@PathParam("id") Long id, Map<String, Object> input) {
        NotificationTemplate existing = em.find(NotificationTemplate.class, id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if (input.containsKey("name")) existing.setName((String) input.get("name"));
        if (input.containsKey("subject")) existing.setSubject((String) input.get("subject"));
        if (input.containsKey("bodyText")) existing.setBodyText((String) input.get("bodyText"));
        if (input.containsKey("channel")) existing.setChannel((String) input.get("channel"));
        if (input.containsKey("active")) existing.setActive((Boolean) input.get("active"));
        em.merge(existing);
        return Response.ok(toMap(existing)).build();
    }

    @PUT
    @Path("/{id}/toggle")
    @Transactional
    public Response toggleActive(@PathParam("id") Long id) {
        NotificationTemplate existing = em.find(NotificationTemplate.class, id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        existing.setActive(!existing.isActive());
        em.merge(existing);
        return Response.ok(toMap(existing)).build();
    }

    private Map<String, Object> toMap(NotificationTemplate t) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", t.getId());
        map.put("templateCode", t.getTemplateCode());
        map.put("name", t.getName());
        map.put("description", t.getDescription());
        map.put("channel", t.getChannel());
        map.put("eventType", t.getEventType());
        map.put("subject", t.getSubject());
        map.put("bodyText", t.getBodyText());
        map.put("active", t.isActive());
        map.put("locale", t.getLocale());
        return map;
    }
}
