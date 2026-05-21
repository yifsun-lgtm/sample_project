package com.proquip.web.resource;

import com.proquip.ejb.entity.organization.DelegationRule;
import com.proquip.ejb.entity.organization.UserProfile;

import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Path("/delegations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMIN", "MANAGER"})
public class DelegationResource {

    private static final Logger logger = Logger.getLogger(DelegationResource.class.getName());

    @PersistenceContext
    private EntityManager em;

    @GET
    public Response listDelegations() {
        List<DelegationRule> list = em.createQuery(
                "SELECT dr FROM DelegationRule dr ORDER BY dr.validFrom DESC", DelegationRule.class)
                .getResultList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (DelegationRule dr : list) {
            result.add(toMap(dr));
        }
        return Response.ok(result).build();
    }

    @GET
    @Path("/{id}")
    public Response getDelegation(@PathParam("id") Long id) {
        DelegationRule dr = em.find(DelegationRule.class, id);
        if (dr == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(toMap(dr)).build();
    }

    @POST
    @Transactional
    public Response createDelegation(Map<String, Object> input) {
        DelegationRule dr = new DelegationRule();
        try {
            applyInput(dr, input);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage())).build();
        }
        em.persist(dr);
        return Response.status(Response.Status.CREATED).entity(toMap(dr)).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Response updateDelegation(@PathParam("id") Long id, Map<String, Object> input) {
        DelegationRule existing = em.find(DelegationRule.class, id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        try {
            applyInput(existing, input);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage())).build();
        }
        em.merge(existing);
        return Response.ok(toMap(existing)).build();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response deleteDelegation(@PathParam("id") Long id) {
        DelegationRule existing = em.find(DelegationRule.class, id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        em.remove(existing);
        return Response.noContent().build();
    }

    @GET
    @Path("/users")
    public Response listUsers() {
        List<UserProfile> users = em.createQuery(
                "SELECT u FROM UserProfile u ORDER BY u.lastName, u.firstName", UserProfile.class)
                .getResultList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserProfile u : users) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", u.getId());
            map.put("employeeNumber", u.getEmployeeNumber());
            map.put("name", u.getLastName() + " " + u.getFirstName());
            result.add(map);
        }
        return Response.ok(result).build();
    }

    private Map<String, Object> toMap(DelegationRule dr) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", dr.getId());
        map.put("delegateFromId", dr.getDelegateFrom() != null ? dr.getDelegateFrom().getId() : null);
        map.put("delegateFromName", dr.getDelegateFrom() != null
                ? dr.getDelegateFrom().getLastName() + " " + dr.getDelegateFrom().getFirstName() : null);
        map.put("delegateToId", dr.getDelegateTo() != null ? dr.getDelegateTo().getId() : null);
        map.put("delegateToName", dr.getDelegateTo() != null
                ? dr.getDelegateTo().getLastName() + " " + dr.getDelegateTo().getFirstName() : null);
        map.put("scope", dr.getScope());
        map.put("validFrom", dr.getValidFrom());
        map.put("validTo", dr.getValidTo());
        return map;
    }

    private void applyInput(DelegationRule dr, Map<String, Object> input) {
        Object fromId = input.get("delegateFromId");
        Object toId = input.get("delegateToId");
        if (fromId == null || toId == null) {
            throw new IllegalArgumentException("委譲元と委譲先のユーザーは必須です。");
        }
        Long fromUserId = ((Number) fromId).longValue();
        Long toUserId = ((Number) toId).longValue();
        if (fromUserId.equals(toUserId)) {
            throw new IllegalArgumentException("委譲元と委譲先は異なるユーザーである必要があります。");
        }
        UserProfile fromUser = em.find(UserProfile.class, fromUserId);
        UserProfile toUser = em.find(UserProfile.class, toUserId);
        if (fromUser == null || toUser == null) {
            throw new IllegalArgumentException("指定されたユーザーが見つかりません。");
        }
        dr.setDelegateFrom(fromUser);
        dr.setDelegateTo(toUser);
        dr.setDelegateFromUser(fromUser);
        dr.setDelegateToUser(toUser);

        String scopeVal = (String) input.get("scope");
        dr.setScope(scopeVal);
        dr.setDelegationType(scopeVal);
        dr.setIsActive(true);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            String vf = (String) input.get("validFrom");
            String vt = (String) input.get("validTo");
            if (vf != null) {
                Date fromDate = sdf.parse(vf);
                dr.setValidFrom(fromDate);
            }
            if (vt != null) {
                Date toDate = sdf.parse(vt);
                dr.setValidTo(toDate);
                dr.setValidUntil(toDate);
            }
        } catch (ParseException e) {
            throw new IllegalArgumentException("日付形式が正しくありません。yyyy-MM-dd形式で指定してください。");
        }
    }
}
