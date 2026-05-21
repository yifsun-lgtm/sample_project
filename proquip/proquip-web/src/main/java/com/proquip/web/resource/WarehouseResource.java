package com.proquip.web.resource;

import com.proquip.common.dto.PageResultDto;
import com.proquip.common.dto.WarehouseDto;
import com.proquip.ejb.entity.inventory.InventoryItem;
import com.proquip.ejb.entity.inventory.StorageLocation;
import com.proquip.ejb.entity.inventory.Warehouse;
import com.proquip.ejb.entity.inventory.WarehouseZone;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 倉庫管理RESTリソース。
 *
 * <p>倉庫のCRUD操作、ゾーン情報、利用率の参照を提供する。
 * 比較的クリーンなコードであり、意図的な技術的負債は少ない。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
@Path("/warehouses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WarehouseResource {

    private static final Logger logger = Logger.getLogger(WarehouseResource.class.getName());

    @PersistenceContext
    private EntityManager em;

    // ========================================================================
    // CRUD
    // ========================================================================

    /**
     * 倉庫一覧を取得する。
     *
     * @param page ページ番号（0始まり）
     * @param size ページサイズ
     * @return 倉庫一覧
     */
    @GET
    @SuppressWarnings("unchecked")
    public Response listWarehouses(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {

        logger.info("倉庫一覧取得。page=" + page + ", size=" + size);

        List<Warehouse> warehouses = em.createQuery(
                "SELECT w FROM Warehouse w ORDER BY w.name")
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();

        long totalElements = (Long) em.createQuery("SELECT COUNT(w) FROM Warehouse w")
                .getSingleResult();

        List<WarehouseDto> dtoList = new ArrayList<>();
        for (int i = 0; i < warehouses.size(); i++) {
            dtoList.add(toDto(warehouses.get(i)));
        }

        PageResultDto<WarehouseDto> pageResult = new PageResultDto<>(dtoList, totalElements, page, size);
        return Response.ok(pageResult).build();
    }

    /**
     * 全倉庫を取得する（ページネーションなし）。
     *
     * @return 倉庫一覧
     */
    @GET
    @Path("/all")
    @SuppressWarnings("unchecked")
    public Response getAllWarehouses() {
        logger.info("全倉庫一覧取得。");

        List<Warehouse> warehouses = em.createQuery(
                "SELECT w FROM Warehouse w ORDER BY w.name")
                .getResultList();

        List<WarehouseDto> dtoList = new ArrayList<>();
        for (int i = 0; i < warehouses.size(); i++) {
            dtoList.add(toDto(warehouses.get(i)));
        }

        return Response.ok(dtoList).build();
    }

    /**
     * 倉庫詳細を取得する。
     *
     * @param id 倉庫ID
     * @return 倉庫DTO
     */
    @GET
    @Path("/{id}")
    public Response getWarehouse(@PathParam("id") Long id) {
        logger.info("倉庫詳細取得。ID=" + id);

        Warehouse warehouse = em.find(Warehouse.class, id);
        if (warehouse == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"倉庫が見つかりません。ID: " + id + "\"}")
                    .build();
        }

        WarehouseDto dto = toDto(warehouse);
        return Response.ok(dto).build();
    }

    /**
     * 倉庫を新規作成する。
     *
     * @param dto    倉庫DTO
     * @param secCtx セキュリティコンテキスト
     * @return 作成された倉庫DTO
     */
    @POST
    @Transactional
    public Response createWarehouse(WarehouseDto dto, @Context SecurityContext secCtx) {
        logger.info("倉庫作成。コード=" + dto.getCode()
                + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        Warehouse warehouse = new Warehouse();
        warehouse.setCode(dto.getCode());
        warehouse.setName(dto.getName());
        warehouse.setAddress(dto.getAddress() != null ? dto.getAddress() : "-");
        warehouse.setCapacity(dto.getCapacity());
        warehouse.setActive(true);

        em.persist(warehouse);

        WarehouseDto resultDto = toDto(warehouse);
        return Response.status(Response.Status.CREATED).entity(resultDto).build();
    }

    /**
     * 倉庫を更新する。
     *
     * @param id     倉庫ID
     * @param dto    更新データ
     * @param secCtx セキュリティコンテキスト
     * @return 更新後の倉庫DTO
     */
    @PUT
    @Path("/{id}")
    @Transactional
    public Response updateWarehouse(@PathParam("id") Long id, WarehouseDto dto,
                                    @Context SecurityContext secCtx) {
        logger.info("倉庫更新。ID=" + id + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        Warehouse warehouse = em.find(Warehouse.class, id);
        if (warehouse == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"倉庫が見つかりません。ID: " + id + "\"}")
                    .build();
        }

        if (dto.getName() != null) {
            warehouse.setName(dto.getName());
        }
        if (dto.getAddress() != null) {
            warehouse.setAddress(dto.getAddress());
        }
        if (dto.getCapacity() != null) {
            warehouse.setCapacity(dto.getCapacity());
        }

        em.merge(warehouse);

        WarehouseDto resultDto = toDto(warehouse);
        return Response.ok(resultDto).build();
    }

    /**
     * 倉庫を削除する。
     */
    @DELETE
    @Path("/{id}")
    @Transactional
    public Response deleteWarehouse(@PathParam("id") Long id, @Context SecurityContext secCtx) {
        logger.info("倉庫削除。ID=" + id + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        Warehouse warehouse = em.find(Warehouse.class, id);
        if (warehouse == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"倉庫が見つかりません。ID: " + id + "\"}")
                    .build();
        }

        Long itemCount = (Long) em.createQuery(
                "SELECT COUNT(i) FROM InventoryItem i WHERE i.warehouse.id = :warehouseId")
                .setParameter("warehouseId", id)
                .getSingleResult();
        if (itemCount > 0) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\":\"在庫アイテムが存在するため削除できません。\"}")
                    .build();
        }

        em.remove(warehouse);
        return Response.noContent().build();
    }

    // ========================================================================
    // ゾーン・利用率
    // ========================================================================

    /**
     * 倉庫のゾーン一覧を取得する。
     *
     * @param id 倉庫ID
     * @return ゾーン一覧
     */
    @GET
    @Path("/{id}/zones")
    @SuppressWarnings("unchecked")
    public Response getWarehouseZones(@PathParam("id") Long id) {
        logger.info("倉庫ゾーン取得。倉庫ID=" + id);

        List<WarehouseZone> zones = em.createQuery(
                "SELECT z FROM WarehouseZone z LEFT JOIN FETCH z.storageLocations WHERE z.warehouse.id = :warehouseId ORDER BY z.name",
                WarehouseZone.class)
                .setParameter("warehouseId", id)
                .getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (WarehouseZone z : zones) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", z.getId());
            map.put("code", z.getCode());
            map.put("name", z.getName());
            map.put("type", z.getZoneType());
            map.put("capacity", 100);
            map.put("currentOccupancy", z.getStorageLocations() != null ? z.getStorageLocations().size() : 0);
            map.put("itemCount", z.getStorageLocations() != null ? z.getStorageLocations().size() : 0);
            map.put("description", z.getName());
            result.add(map);
        }

        return Response.ok(result).build();
    }

    /**
     * ゾーン内の在庫品目を取得する。
     *
     * @param id     倉庫ID
     * @param zoneId ゾーンID
     * @return 在庫品目一覧
     */
    @GET
    @Path("/{id}/zones/{zoneId}/items")
    public Response getZoneItems(@PathParam("id") Long id, @PathParam("zoneId") Long zoneId) {
        logger.info("ゾーン在庫品目取得。倉庫ID=" + id + ", ゾーンID=" + zoneId);

        List<InventoryItem> items = em.createQuery(
                "SELECT ii FROM InventoryItem ii JOIN FETCH ii.product WHERE ii.warehouse.id = :warehouseId",
                InventoryItem.class)
                .setParameter("warehouseId", id)
                .setMaxResults(50)
                .getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (InventoryItem ii : items) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", ii.getId());
            item.put("productName", ii.getProduct().getName());
            item.put("productSku", ii.getProduct().getSku());
            item.put("quantity", ii.getQuantityOnHand());
            item.put("quantityReserved", ii.getQuantityReserved());
            result.add(item);
        }

        return Response.ok(result).build();
    }

    /**
     * 倉庫の利用率を取得する。
     *
     * @param id 倉庫ID
     * @return 利用率情報
     */
    @GET
    @Path("/{id}/utilization")
    @SuppressWarnings("unchecked")
    public Response getWarehouseUtilization(@PathParam("id") Long id) {
        logger.info("倉庫利用率取得。倉庫ID=" + id);

        Warehouse warehouse = em.find(Warehouse.class, id);
        if (warehouse == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"倉庫が見つかりません。ID: " + id + "\"}")
                    .build();
        }

        // 在庫品目数の集計
        Long itemCount = (Long) em.createQuery(
                "SELECT COUNT(i) FROM InventoryItem i WHERE i.warehouse.id = :warehouseId")
                .setParameter("warehouseId", id)
                .getSingleResult();

        Object totalQtyObj = em.createQuery(
                "SELECT COALESCE(SUM(i.quantityOnHand), 0) FROM InventoryItem i " +
                "WHERE i.warehouse.id = :warehouseId")
                .setParameter("warehouseId", id)
                .getSingleResult();

        Map<String, Object> utilization = new HashMap<>();
        utilization.put("warehouseId", id);
        utilization.put("warehouseName", warehouse.getName());
        utilization.put("capacity", warehouse.getCapacity());
        utilization.put("itemCount", itemCount);
        utilization.put("totalQuantity", totalQtyObj);

        if (warehouse.getCapacity() != null && warehouse.getCapacity() > 0) {
            long totalQty = ((Number) totalQtyObj).longValue();
            double rate = (double) totalQty / warehouse.getCapacity() * 100;
            utilization.put("utilizationPercentage", Math.round(rate * 100.0) / 100.0);
        } else {
            utilization.put("utilizationPercentage", 0.0);
        }

        return Response.ok(utilization).build();
    }

    // ========================================================================
    // ゾーンCRUD
    // ========================================================================

    @POST
    @Path("/{id}/zones")
    @Transactional
    public Response createZone(@PathParam("id") Long warehouseId, Map<String, Object> body,
                                @Context SecurityContext secCtx) {
        logger.info("ゾーン作成。倉庫ID=" + warehouseId);

        Warehouse warehouse = em.find(Warehouse.class, warehouseId);
        if (warehouse == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"倉庫が見つかりません\"}").build();
        }

        String code = (String) body.get("code");
        String name = (String) body.get("name");
        String zoneType = (String) body.getOrDefault("zoneType", "GENERAL");

        if (code == null || name == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"code と name は必須です\"}").build();
        }

        WarehouseZone zone = new WarehouseZone();
        zone.setCode(code);
        zone.setName(name);
        zone.setZoneType(zoneType);
        zone.setWarehouse(warehouse);
        zone.setActive(true);

        em.persist(zone);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", zone.getId());
        result.put("code", zone.getCode());
        result.put("name", zone.getName());
        result.put("type", zone.getZoneType());

        return Response.status(Response.Status.CREATED).entity(result).build();
    }

    @PUT
    @Path("/{id}/zones/{zoneId}")
    @Transactional
    public Response updateZone(@PathParam("id") Long warehouseId, @PathParam("zoneId") Long zoneId,
                                Map<String, Object> body) {
        logger.info("ゾーン更新。倉庫ID=" + warehouseId + ", ゾーンID=" + zoneId);

        WarehouseZone zone = em.find(WarehouseZone.class, zoneId);
        if (zone == null || !zone.getWarehouse().getId().equals(warehouseId)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"ゾーンが見つかりません\"}").build();
        }

        if (body.containsKey("name")) {
            zone.setName((String) body.get("name"));
        }
        if (body.containsKey("zoneType")) {
            zone.setZoneType((String) body.get("zoneType"));
        }
        if (body.containsKey("code")) {
            zone.setCode((String) body.get("code"));
        }

        em.merge(zone);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", zone.getId());
        result.put("code", zone.getCode());
        result.put("name", zone.getName());
        result.put("type", zone.getZoneType());

        return Response.ok(result).build();
    }

    @DELETE
    @Path("/{id}/zones/{zoneId}")
    @Transactional
    public Response deleteZone(@PathParam("id") Long warehouseId, @PathParam("zoneId") Long zoneId) {
        logger.info("ゾーン削除。倉庫ID=" + warehouseId + ", ゾーンID=" + zoneId);

        WarehouseZone zone = em.find(WarehouseZone.class, zoneId);
        if (zone == null || !zone.getWarehouse().getId().equals(warehouseId)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"ゾーンが見つかりません\"}").build();
        }

        em.remove(zone);
        return Response.noContent().build();
    }

    // ========================================================================
    // ロケーションCRUD
    // ========================================================================

    @GET
    @Path("/{id}/zones/{zoneId}/locations")
    @SuppressWarnings("unchecked")
    public Response getZoneLocations(@PathParam("id") Long warehouseId, @PathParam("zoneId") Long zoneId) {
        logger.info("ロケーション一覧取得。ゾーンID=" + zoneId);

        List<StorageLocation> locations = em.createQuery(
                "SELECT sl FROM StorageLocation sl WHERE sl.zone.id = :zoneId ORDER BY sl.code",
                StorageLocation.class)
                .setParameter("zoneId", zoneId)
                .getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (StorageLocation sl : locations) {
            result.add(toLocationDto(sl));
        }

        return Response.ok(result).build();
    }

    @POST
    @Path("/{id}/zones/{zoneId}/locations")
    @Transactional
    public Response createLocation(@PathParam("id") Long warehouseId, @PathParam("zoneId") Long zoneId,
                                    Map<String, Object> body) {
        logger.info("ロケーション作成。ゾーンID=" + zoneId);

        WarehouseZone zone = em.find(WarehouseZone.class, zoneId);
        if (zone == null || !zone.getWarehouse().getId().equals(warehouseId)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"ゾーンが見つかりません\"}").build();
        }

        String code = (String) body.get("code");
        if (code == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"code は必須です\"}").build();
        }

        StorageLocation loc = new StorageLocation();
        loc.setCode(code);
        loc.setAisle((String) body.get("aisle"));
        loc.setRack((String) body.get("rack"));
        loc.setShelf((String) body.get("shelf"));
        loc.setBin((String) body.get("bin"));
        loc.setLocationType((String) body.getOrDefault("locationType", "SHELF"));
        loc.setZone(zone);
        loc.setActive(true);

        em.persist(loc);

        return Response.status(Response.Status.CREATED).entity(toLocationDto(loc)).build();
    }

    @PUT
    @Path("/{id}/zones/{zoneId}/locations/{locId}")
    @Transactional
    public Response updateLocation(@PathParam("id") Long warehouseId, @PathParam("zoneId") Long zoneId,
                                    @PathParam("locId") Long locId, Map<String, Object> body) {
        logger.info("ロケーション更新。ID=" + locId);

        StorageLocation loc = em.find(StorageLocation.class, locId);
        if (loc == null || !loc.getZone().getId().equals(zoneId)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"ロケーションが見つかりません\"}").build();
        }

        if (body.containsKey("code")) loc.setCode((String) body.get("code"));
        if (body.containsKey("aisle")) loc.setAisle((String) body.get("aisle"));
        if (body.containsKey("rack")) loc.setRack((String) body.get("rack"));
        if (body.containsKey("shelf")) loc.setShelf((String) body.get("shelf"));
        if (body.containsKey("bin")) loc.setBin((String) body.get("bin"));
        if (body.containsKey("locationType")) loc.setLocationType((String) body.get("locationType"));

        em.merge(loc);

        return Response.ok(toLocationDto(loc)).build();
    }

    @DELETE
    @Path("/{id}/zones/{zoneId}/locations/{locId}")
    @Transactional
    public Response deleteLocation(@PathParam("id") Long warehouseId, @PathParam("zoneId") Long zoneId,
                                    @PathParam("locId") Long locId) {
        logger.info("ロケーション削除。ID=" + locId);

        StorageLocation loc = em.find(StorageLocation.class, locId);
        if (loc == null || !loc.getZone().getId().equals(zoneId)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"ロケーションが見つかりません\"}").build();
        }

        em.remove(loc);
        return Response.noContent().build();
    }

    private Map<String, Object> toLocationDto(StorageLocation sl) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", sl.getId());
        map.put("code", sl.getCode());
        map.put("aisle", sl.getAisle());
        map.put("rack", sl.getRack());
        map.put("shelf", sl.getShelf());
        map.put("bin", sl.getBin());
        map.put("locationType", sl.getLocationType());
        return map;
    }

    // ========================================================================
    // ヘルパーメソッド
    // ========================================================================

    /**
     * 倉庫エンティティをDTOに変換する。
     *
     * @param entity 倉庫エンティティ
     * @return 倉庫DTO
     */
    private WarehouseDto toDto(Warehouse entity) {
        WarehouseDto dto = new WarehouseDto();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setAddress(entity.getAddress());
        dto.setCapacity(entity.getCapacity());
        dto.setActive(entity.isActive());

        Long zoneCount = (Long) em.createQuery(
                "SELECT COUNT(z) FROM WarehouseZone z WHERE z.warehouse.id = :wid")
                .setParameter("wid", entity.getId())
                .getSingleResult();
        dto.setZoneCount(zoneCount.intValue());

        if (entity.getCapacity() != null && entity.getCapacity() > 0) {
            Long locationCount = (Long) em.createQuery(
                    "SELECT COUNT(sl) FROM StorageLocation sl WHERE sl.zone.warehouse.id = :wid")
                    .setParameter("wid", entity.getId())
                    .getSingleResult();
            dto.setUtilizationPercentage(
                    Math.round(locationCount * 100.0 / entity.getCapacity() * 10.0) / 10.0);
        } else {
            dto.setUtilizationPercentage(0.0);
        }

        return dto;
    }
}
