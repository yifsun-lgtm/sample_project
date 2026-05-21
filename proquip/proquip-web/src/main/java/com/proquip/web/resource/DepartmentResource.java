package com.proquip.web.resource;

import com.proquip.ejb.entity.organization.Department;

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

@Path("/departments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMIN", "MANAGER"})
public class DepartmentResource {

    @PersistenceContext
    private EntityManager em;

    @GET
    public Response listDepartments() {
        List<Department> list = em.createQuery(
                "SELECT d FROM Department d ORDER BY d.sortOrder, d.name", Department.class)
                .getResultList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Department d : list) {
            result.add(toMap(d));
        }
        return Response.ok(result).build();
    }

    @GET
    @Path("/{id}")
    public Response getDepartment(@PathParam("id") Long id) {
        Department d = em.find(Department.class, id);
        if (d == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(toMap(d)).build();
    }

    @POST
    @Transactional
    public Response createDepartment(Map<String, Object> input) {
        Department d = new Department();
        applyInput(d, input);
        em.persist(d);
        return Response.status(Response.Status.CREATED).entity(toMap(d)).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Response updateDepartment(@PathParam("id") Long id, Map<String, Object> input) {
        Department existing = em.find(Department.class, id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        applyInput(existing, input);
        em.merge(existing);
        return Response.ok(toMap(existing)).build();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response deleteDepartment(@PathParam("id") Long id) {
        Department existing = em.find(Department.class, id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        em.remove(existing);
        return Response.noContent().build();
    }

    private Map<String, Object> toMap(Department d) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", d.getId());
        map.put("code", d.getCode());
        map.put("name", d.getName());
        map.put("costCenter", d.getCostCenter());
        map.put("level", d.getLevel());
        map.put("sortOrder", d.getSortOrder());
        map.put("active", d.isActive());
        map.put("parentId", d.getParent() != null ? d.getParent().getId() : null);
        map.put("parentName", d.getParent() != null ? d.getParent().getName() : null);
        return map;
    }

    private void applyInput(Department d, Map<String, Object> input) {
        if (input.containsKey("code")) d.setCode((String) input.get("code"));
        if (input.containsKey("name")) d.setName((String) input.get("name"));
        if (input.containsKey("costCenter")) d.setCostCenter((String) input.get("costCenter"));
        if (input.containsKey("sortOrder")) d.setSortOrder(((Number) input.get("sortOrder")).intValue());
        if (input.containsKey("active")) d.setActive((Boolean) input.get("active"));

        if (input.containsKey("parentId")) {
            Object pid = input.get("parentId");
            if (pid != null) {
                Long parentId = ((Number) pid).longValue();
                Department parent = em.find(Department.class, parentId);
                d.setParent(parent);
                d.setLevel(parent != null ? parent.getLevel() + 1 : 0);
            } else {
                d.setParent(null);
                d.setLevel(0);
            }
        }
    }
}
