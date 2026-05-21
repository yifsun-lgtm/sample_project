package com.proquip.web.resource;

import com.proquip.ejb.entity.procurement.ReturnToSupplier;
import com.proquip.ejb.entity.supplier.Supplier;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Path("/returns")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReturnResource {

    private static final Logger logger = Logger.getLogger(ReturnResource.class.getName());

    @PersistenceContext
    private EntityManager em;

    @GET
    public Response listReturns(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {

        logger.info("返品一覧取得。page=" + page + ", size=" + size);

        List<ReturnToSupplier> returns = em.createQuery(
                "SELECT r FROM ReturnToSupplier r LEFT JOIN FETCH r.supplier ORDER BY r.returnDate DESC",
                ReturnToSupplier.class)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();

        long totalElements = (Long) em.createQuery(
                "SELECT COUNT(r) FROM ReturnToSupplier r")
                .getSingleResult();

        List<Map<String, Object>> dtoList = new ArrayList<>();
        for (ReturnToSupplier r : returns) {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id", r.getId());
            dto.put("returnNumber", r.getReturnNumber());
            dto.put("orderId", r.getPurchaseOrderId());

            String orderNumber = "";
            if (r.getPurchaseOrderId() != null) {
                try {
                    Object on = em.createQuery(
                            "SELECT po.orderNumber FROM PurchaseOrder po WHERE po.id = :id")
                            .setParameter("id", r.getPurchaseOrderId())
                            .getSingleResult();
                    orderNumber = on != null ? on.toString() : "";
                } catch (Exception e) {
                    logger.fine("発注番号取得失敗。orderId=" + r.getPurchaseOrderId());
                }
            }
            dto.put("orderNumber", orderNumber);
            dto.put("supplierName", r.getSupplier() != null ? r.getSupplier().getName() : "");
            dto.put("status", r.getStatus());
            dto.put("reason", r.getReason());
            dto.put("totalAmount", r.getCreditAmount());
            dto.put("createdAt", r.getCreatedAt());
            dto.put("items", new ArrayList<>());
            dtoList.add(dto);
        }

        Map<String, Object> pageResult = new LinkedHashMap<>();
        pageResult.put("content", dtoList);
        pageResult.put("totalElements", totalElements);
        pageResult.put("page", page);
        pageResult.put("size", size);
        pageResult.put("totalPages", (int) Math.ceil((double) totalElements / size));

        return Response.ok(pageResult).build();
    }

    @POST
    @Transactional
    @SuppressWarnings("unchecked")
    public Response createReturn(Map<String, Object> body, @Context SecurityContext secCtx) {
        logger.info("返品作成。ユーザー=" + secCtx.getUserPrincipal().getName());

        Number orderId = (Number) body.get("orderId");
        String reason = (String) body.get("reason");
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");

        if (orderId == null || reason == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"orderId と reason は必須です\"}")
                    .build();
        }

        Long supplierId;
        try {
            supplierId = (Long) em.createQuery(
                    "SELECT po.supplier.id FROM PurchaseOrder po WHERE po.id = :id")
                    .setParameter("id", orderId.longValue())
                    .getSingleResult();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"指定された発注が見つかりません\"}")
                    .build();
        }

        Supplier supplier = em.find(Supplier.class, supplierId);

        Long userId = resolveUserId(secCtx);

        String returnNumber = "RET-" + new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());

        BigDecimal totalAmount = BigDecimal.ZERO;
        if (items != null) {
            for (Map<String, Object> item : items) {
                Number qty = (Number) item.get("quantity");
                Number price = (Number) item.get("unitPrice");
                if (qty != null && price != null) {
                    totalAmount = totalAmount.add(
                            new BigDecimal(qty.toString()).multiply(new BigDecimal(price.toString())));
                }
            }
        }

        ReturnToSupplier ret = new ReturnToSupplier();
        ret.setReturnNumber(returnNumber);
        ret.setReturnDate(new Date());
        ret.setReason(reason);
        ret.setStatus("DRAFT");
        ret.setCreditAmount(totalAmount);
        ret.setSupplier(supplier);
        ret.setPurchaseOrderId(orderId.longValue());
        ret.setInitiatedBy(userId);

        em.persist(ret);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", ret.getId());
        result.put("returnNumber", ret.getReturnNumber());
        result.put("status", ret.getStatus());

        return Response.status(Response.Status.CREATED).entity(result).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Response updateReturn(@PathParam("id") Long id, Map<String, Object> body,
                                  @Context SecurityContext secCtx) {
        logger.info("返品更新。ID=" + id + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        ReturnToSupplier ret = em.find(ReturnToSupplier.class, id);
        if (ret == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"返品が見つかりません。ID: " + id + "\"}")
                    .build();
        }

        if (body.containsKey("status")) {
            ret.setStatus((String) body.get("status"));
        }
        if (body.containsKey("reason")) {
            ret.setReason((String) body.get("reason"));
        }
        if (body.containsKey("creditAmount")) {
            Number amt = (Number) body.get("creditAmount");
            if (amt != null) {
                ret.setCreditAmount(new BigDecimal(amt.toString()));
            }
        }

        em.merge(ret);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", ret.getId());
        result.put("returnNumber", ret.getReturnNumber());
        result.put("status", ret.getStatus());

        return Response.ok(result).build();
    }

    private Long resolveUserId(SecurityContext secCtx) {
        try {
            String keycloakId = secCtx.getUserPrincipal().getName();
            return (Long) em.createQuery(
                    "SELECT u.id FROM UserProfile u WHERE u.keycloakId = :kid")
                    .setParameter("kid", keycloakId)
                    .getSingleResult();
        } catch (Exception e) {
            return 1L;
        }
    }
}
