package com.proquip.web.resource;

import com.proquip.common.dto.PageResultDto;
import com.proquip.common.dto.pricing.PriceListDto;
import com.proquip.ejb.entity.pricing.PriceList;

import com.proquip.ejb.entity.pricing.PriceListItem;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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

import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Path("/price-lists")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PriceListResource {

    private static final Logger logger = Logger.getLogger(PriceListResource.class.getName());

    @PersistenceContext
    private EntityManager em;

    @GET
    @SuppressWarnings("unchecked")
    public Response listPriceLists(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("status") String status,
            @QueryParam("keyword") String keyword) {

        logger.info("価格表一覧取得。page=" + page + ", size=" + size);

        StringBuilder jpql = new StringBuilder("SELECT p FROM PriceList p WHERE 1=1");
        StringBuilder countJpql = new StringBuilder("SELECT COUNT(p) FROM PriceList p WHERE 1=1");

        if (status != null && !status.isEmpty()) {
            jpql.append(" AND p.status = :status");
            countJpql.append(" AND p.status = :status");
        }
        if (keyword != null && !keyword.isEmpty()) {
            jpql.append(" AND LOWER(p.name) LIKE :keyword");
            countJpql.append(" AND LOWER(p.name) LIKE :keyword");
        }
        jpql.append(" ORDER BY p.name");

        var query = em.createQuery(jpql.toString());
        var cQuery = em.createQuery(countJpql.toString());

        if (status != null && !status.isEmpty()) {
            query.setParameter("status", status);
            cQuery.setParameter("status", status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            query.setParameter("keyword", "%" + keyword.toLowerCase() + "%");
            cQuery.setParameter("keyword", "%" + keyword.toLowerCase() + "%");
        }

        query.setFirstResult(page * size);
        query.setMaxResults(size);

        List<PriceList> priceLists = query.getResultList();
        long totalElements = (Long) cQuery.getSingleResult();

        List<PriceListDto> dtoList = new ArrayList<>();
        for (PriceList pl : priceLists) {
            dtoList.add(toDto(pl));
        }

        PageResultDto<PriceListDto> pageResult = new PageResultDto<>(dtoList, totalElements, page, size);
        return Response.ok(pageResult).build();
    }

    @GET
    @Path("/{id}")
    public Response getPriceList(@PathParam("id") Long id) {
        logger.info("価格表詳細取得。ID=" + id);

        PriceList priceList = em.find(PriceList.class, id);
        if (priceList == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"価格表が見つかりません。ID: " + id + "\"}")
                    .build();
        }

        PriceListDto dto = toDto(priceList);
        return Response.ok(dto).build();
    }

    @POST
    @Transactional
    public Response createPriceList(Map<String, Object> body,
                                    @Context SecurityContext secCtx) {
        logger.info("価格表作成。ユーザー=" + secCtx.getUserPrincipal().getName());

        PriceList priceList = new PriceList();
        priceList.setName(body.get("name") != null ? body.get("name").toString() : null);
        priceList.setCurrency(body.get("currency") != null ? body.get("currency").toString() : "JPY");
        priceList.setStatus(body.get("status") != null ? body.get("status").toString() : "DRAFT");
        priceList.setDescription(body.get("description") != null ? body.get("description").toString() : null);
        priceList.setPriceListCode("PL-" + System.currentTimeMillis());
        priceList.setPriceListType(body.get("priceListType") != null ? body.get("priceListType").toString() : "STANDARD");

        Object fromVal = body.get("effectiveFrom");
        if (fromVal == null) fromVal = body.get("effectiveStartDate");
        if (fromVal != null) {
            priceList.setEffectiveFrom(parseDate(fromVal.toString()));
        }
        Object toVal = body.get("effectiveTo");
        if (toVal == null) toVal = body.get("effectiveEndDate");
        if (toVal != null) {
            priceList.setEffectiveTo(parseDate(toVal.toString()));
        }

        em.persist(priceList);

        return Response.status(Response.Status.CREATED).entity(toDto(priceList)).build();
    }

    @PUT
    @Path("/{id}")
    public Response updatePriceList(@PathParam("id") Long id,
                                    Map<String, Object> body,
                                    @Context SecurityContext secCtx) {
        logger.info("価格表更新。ID=" + id + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        PriceList priceList = em.find(PriceList.class, id);
        if (priceList == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"価格表が見つかりません。ID: " + id + "\"}")
                    .build();
        }

        if (body.get("name") != null) priceList.setName(body.get("name").toString());
        if (body.get("currency") != null) priceList.setCurrency(body.get("currency").toString());
        if (body.get("status") != null) priceList.setStatus(body.get("status").toString());
        if (body.get("effectiveFrom") != null) {
            priceList.setEffectiveFrom(parseDate(body.get("effectiveFrom").toString()));
        }
        if (body.get("effectiveTo") != null) {
            priceList.setEffectiveTo(parseDate(body.get("effectiveTo").toString()));
        }

        em.merge(priceList);

        return Response.ok(toDto(priceList)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deletePriceList(@PathParam("id") Long id) {
        logger.info("価格表削除（論理）。ID=" + id);

        PriceList priceList = em.find(PriceList.class, id);
        if (priceList == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"価格表が見つかりません。ID: " + id + "\"}")
                    .build();
        }

        priceList.setStatus("ARCHIVED");
        em.merge(priceList);

        return Response.noContent().build();
    }

    @GET
    @Path("/{id}/items")
    @SuppressWarnings("unchecked")
    public Response getPriceListItems(@PathParam("id") Long id) {
        logger.info("価格表アイテム取得。ID=" + id);

        PriceList priceList = em.find(PriceList.class, id);
        if (priceList == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"価格表が見つかりません。ID: " + id + "\"}")
                    .build();
        }

        List<PriceListItem> items = em.createQuery(
                "SELECT pli FROM PriceListItem pli LEFT JOIN FETCH pli.product WHERE pli.priceList.id = :id ORDER BY pli.product.name")
                .setParameter("id", id)
                .getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (PriceListItem item : items) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", item.getId());
            map.put("productId", item.getProduct().getId());
            map.put("productName", item.getProduct().getName());
            map.put("sku", item.getProduct().getSku());
            map.put("unitPrice", item.getUnitPrice());
            map.put("standardUnitPrice", item.getUnitPrice());
            map.put("standardPrice", item.getUnitPrice());
            map.put("listPrice", item.getUnitPrice());
            map.put("discountRate", 0);
            map.put("taxRate", 10);
            map.put("taxIncludedPrice", item.getUnitPrice() != null
                    ? Math.round(item.getUnitPrice().doubleValue() * 1.1) : 0);
            map.put("minQuantity", item.getMinQuantity());
            map.put("maxQuantity", item.getMaxQuantity());
            map.put("effectiveStartDate", priceList.getEffectiveFrom() != null
                    ? new SimpleDateFormat("yyyy-MM-dd").format(priceList.getEffectiveFrom()) : null);
            map.put("effectiveEndDate", priceList.getEffectiveTo() != null
                    ? new SimpleDateFormat("yyyy-MM-dd").format(priceList.getEffectiveTo()) : null);
            result.add(map);
        }

        return Response.ok(result).build();
    }

    @PUT
    @Path("/{id}/items")
    @Transactional
    @SuppressWarnings("unchecked")
    public Response updatePriceListItems(@PathParam("id") Long id,
                                         List<Map<String, Object>> items,
                                         @Context SecurityContext secCtx) {
        logger.info("価格表アイテム一括更新。ID=" + id + ", 件数=" + items.size()
                + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        PriceList priceList = em.find(PriceList.class, id);
        if (priceList == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"価格表が見つかりません。ID: " + id + "\"}")
                    .build();
        }

        for (Map<String, Object> itemData : items) {
            Object itemIdObj = itemData.get("id");
            if (itemIdObj == null) continue;

            Long itemId = ((Number) itemIdObj).longValue();
            PriceListItem pli = em.find(PriceListItem.class, itemId);
            if (pli == null || !pli.getPriceList().getId().equals(id)) {
                continue;
            }

            Object listPriceObj = itemData.get("listPrice");
            if (listPriceObj != null) {
                pli.setUnitPrice(new BigDecimal(listPriceObj.toString()));
            }

            em.merge(pli);
        }

        return Response.ok("{\"message\":\"価格表アイテムを更新しました。\"}").build();
    }

    private java.util.Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(dateStr);
        } catch (ParseException e) {
            logger.warning("日付パースエラー: " + dateStr);
            return null;
        }
    }

    private PriceListDto toDto(PriceList entity) {
        PriceListDto dto = new PriceListDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCurrency(entity.getCurrency());
        dto.setEffectiveFrom(entity.getEffectiveFrom() != null ? new java.util.Date(entity.getEffectiveFrom().getTime()) : null);
        dto.setEffectiveTo(entity.getEffectiveTo() != null ? new java.util.Date(entity.getEffectiveTo().getTime()) : null);
        dto.setStatus(entity.getStatus());
        dto.setDescription(entity.getDescription());

        try {
            if (entity.getItems() != null) {
                dto.setItemCount(entity.getItems().size());
            }
        } catch (Exception e) {
            dto.setItemCount(0);
        }

        return dto;
    }
}
