package com.proquip.web.resource;

import com.proquip.common.constant.AppConstants;
import com.proquip.common.dto.PageResultDto;
import com.proquip.common.dto.SupplierDTO;
import com.proquip.ejb.mapper.SupplierMapper;
import com.proquip.ejb.entity.supplier.Supplier;
import com.proquip.ejb.entity.supplier.SupplierContact;
import com.proquip.ejb.entity.supplier.SupplierContract;
import com.proquip.ejb.entity.supplier.SupplierRating;
import com.proquip.ejb.service.ProductServiceBean;
import com.proquip.ejb.service.PurchaseOrderServiceBean;
import com.proquip.ejb.service.SupplierServiceBean;

import jakarta.inject.Inject;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.proquip.ejb.entity.supplier.SupplierCertification;
import com.proquip.ejb.entity.supplier.SupplierProduct;
import com.proquip.ejb.entity.organization.UserProfile;

/**
 * 仕入先管理RESTリソース。
 *
 * <p>仕入先のCRUD操作、評価管理、契約管理、比較機能を提供する。</p>
 *
 * <p>【技術的負債 #12 - エンティティ直接返却】
 * {@link #getSupplier(Long)} メソッドでJPAエンティティを直接JSON応答として
 * 返しており、遅延ロード関連のシリアライズ問題が発生するリスクがある。
 * 全てのエンドポイントでDTOを返すべき。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
@Path("/suppliers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SupplierResource {

    private static final Logger logger = Logger.getLogger(SupplierResource.class.getName());

    @PersistenceContext
    private EntityManager em;

    @Inject
    private SupplierServiceBean supplierService;

    @Inject
    private PurchaseOrderServiceBean purchaseOrderService;

    @Inject
    private SupplierMapper supplierMapper;

    // ========================================================================
    // CRUD
    // ========================================================================

    /**
     * 仕入先一覧を取得する（ページネーション付き）。
     *
     * @param page   ページ番号（0始まり）
     * @param size   ページサイズ
     * @param status ステータスフィルタ
     * @return 仕入先一覧のページネーション結果
     */
    @GET
    public Response listSuppliers(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("status") String status) {

        logger.info("仕入先一覧取得。page=" + page + ", size=" + size + ", status=" + status);

        List<Supplier> suppliers;
        if (status != null && !status.isEmpty()) {
            suppliers = supplierService.findByStatus(status);
        } else {
            suppliers = supplierService.findAllSuppliers();
        }

        // 技術的負債: サービス層でページネーションすべき
        int totalCount = suppliers.size();
        if (size > AppConstants.MAX_PAGE_SIZE) {
            size = AppConstants.MAX_PAGE_SIZE;
        }
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalCount);

        List<SupplierDTO> dtoList = new ArrayList<>();
        if (fromIndex < totalCount) {
            List<Supplier> pageSuppliers = suppliers.subList(fromIndex, toIndex);
            for (int i = 0; i < pageSuppliers.size(); i++) {
                dtoList.add(enrichDto(supplierMapper.toDto(pageSuppliers.get(i)), pageSuppliers.get(i)));
            }
        }

        PageResultDto<SupplierDTO> result = new PageResultDto<>(dtoList, totalCount, page, size);
        return Response.ok(result).build();
    }

    /**
     * 仕入先詳細を取得する。
     *
     * <p>【技術的負債 #12】Supplierエンティティを直接返却。
     * 遅延ロードされたコレクション（contracts, ratingsなど）のシリアライズ時に
     * LazyInitializationExceptionが発生するリスクがある。</p>
     *
     * @param id 仕入先ID
     * @return 仕入先エンティティ（技術的負債: DTOを返すべき）
     */
    @GET
    @Path("/{id}")
    public Response getSupplier(@PathParam("id") Long id) {
        logger.info("仕入先詳細取得。ID=" + id);

        Supplier supplier = supplierService.findById(id);
        return Response.ok(enrichDto(supplierMapper.toDto(supplier), supplier)).build();
    }

    /**
     * 仕入先を新規作成する。
     *
     * @param dto    仕入先DTO
     * @param secCtx セキュリティコンテキスト
     * @return 作成された仕入先DTO
     */
    @POST
    public Response createSupplier(SupplierDTO dto, @Context SecurityContext secCtx) {
        logger.info("仕入先作成。コード=" + dto.getCode()
                + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        Supplier entity = supplierMapper.toEntity(dto);
        Supplier created = supplierService.createSupplier(entity);
        SupplierDTO resultDto = supplierMapper.toDto(created);

        return Response.status(Response.Status.CREATED).entity(resultDto).build();
    }

    /**
     * 仕入先を更新する。
     *
     * @param id     仕入先ID
     * @param dto    更新データ
     * @param secCtx セキュリティコンテキスト
     * @return 更新後の仕入先DTO
     */
    @PUT
    @Path("/{id}")
    public Response updateSupplier(@PathParam("id") Long id, SupplierDTO dto,
                                   @Context SecurityContext secCtx) {
        logger.info("仕入先更新。ID=" + id + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        Supplier entity = supplierMapper.toEntity(dto);
        entity.setId(id);
        Supplier updated = supplierService.updateSupplier(entity);
        SupplierDTO resultDto = supplierMapper.toDto(updated);

        return Response.ok(resultDto).build();
    }

    /**
     * 仕入先を論理削除する（ステータスをINACTIVEに変更）。
     *
     * @param id     仕入先ID
     * @param secCtx セキュリティコンテキスト
     * @return 204 No Content
     */
    @DELETE
    @Path("/{id}")
    public Response deleteSupplier(@PathParam("id") Long id, @Context SecurityContext secCtx) {
        logger.info("仕入先削除（論理）。ID=" + id + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        supplierService.deactivateSupplier(id);
        return Response.noContent().build();
    }

    // ========================================================================
    // 関連情報
    // ========================================================================

    /**
     * 仕入先の取扱商品を取得する。
     *
     * @param id 仕入先ID
     * @return 商品一覧
     */
    @GET
    @Path("/{id}/products")
    @SuppressWarnings("unchecked")
    public Response getSupplierProducts(@PathParam("id") Long id) {
        logger.info("仕入先の取扱商品取得。仕入先ID=" + id);

        supplierService.findById(id);

        List<SupplierProduct> spList = em.createQuery(
                "SELECT sp FROM SupplierProduct sp LEFT JOIN FETCH sp.product WHERE sp.supplier.id = :supplierId ORDER BY sp.product.name",
                SupplierProduct.class)
                .setParameter("supplierId", id)
                .getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (SupplierProduct sp : spList) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", sp.getId());
            map.put("supplierSku", sp.getSupplierSku());
            map.put("unitCost", sp.getUnitCost());
            map.put("leadTimeDays", sp.getLeadTimeDays());
            map.put("minOrderQty", sp.getMinOrderQty());
            map.put("isPreferred", sp.isPreferred());
            if (sp.getProduct() != null) {
                map.put("productId", sp.getProduct().getId());
                map.put("productName", sp.getProduct().getName());
                map.put("productSku", sp.getProduct().getSku());
            }
            result.add(map);
        }

        return Response.ok(result).build();
    }

    /**
     * 仕入先の契約一覧を取得する。
     *
     * @param id 仕入先ID
     * @return 契約一覧
     */
    @GET
    @Path("/{id}/contracts")
    public Response getSupplierContracts(@PathParam("id") Long id) {
        logger.info("仕入先の契約取得。仕入先ID=" + id);

        supplierService.findById(id);
        List<SupplierContract> contracts = em.createQuery(
                "SELECT sc FROM SupplierContract sc WHERE sc.supplier.id = :supplierId ORDER BY sc.startDate DESC",
                SupplierContract.class)
                .setParameter("supplierId", id)
                .getResultList();
        return Response.ok(contracts).build();
    }

    @POST
    @Path("/{id}/contracts")
    @Transactional
    public Response createContract(@PathParam("id") Long id, Map<String, Object> data) {
        logger.info("契約作成。仕入先ID=" + id);
        Supplier supplier = supplierService.findById(id);

        SupplierContract contract = new SupplierContract();
        contract.setSupplier(supplier);
        contract.setContractNumber(data.get("contractNumber").toString());
        contract.setTitle(data.get("title") != null ? data.get("title").toString() : data.get("contractNumber").toString());
        contract.setStartDate(parseDate(data.get("startDate")));
        contract.setEndDate(parseDate(data.get("endDate")));
        contract.setStatus(data.get("status") != null ? data.get("status").toString() : "DRAFT");
        contract.setTerms(data.get("terms") != null ? data.get("terms").toString() : null);
        em.persist(contract);

        return Response.status(Response.Status.CREATED).entity(contract).build();
    }

    @PUT
    @Path("/{id}/contracts/{contractId}")
    @Transactional
    public Response updateContract(@PathParam("id") Long id,
                                   @PathParam("contractId") Long contractId,
                                   Map<String, Object> data) {
        logger.info("契約更新。仕入先ID=" + id + ", 契約ID=" + contractId);
        SupplierContract contract = em.find(SupplierContract.class, contractId);
        if (contract == null || !contract.getSupplier().getId().equals(id)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if (data.containsKey("contractNumber")) contract.setContractNumber(data.get("contractNumber").toString());
        if (data.containsKey("title")) contract.setTitle(data.get("title").toString());
        if (data.containsKey("startDate")) contract.setStartDate(parseDate(data.get("startDate")));
        if (data.containsKey("endDate")) contract.setEndDate(parseDate(data.get("endDate")));
        if (data.containsKey("status")) contract.setStatus(data.get("status").toString());
        if (data.containsKey("terms")) contract.setTerms(data.get("terms") != null ? data.get("terms").toString() : null);
        em.merge(contract);

        return Response.ok(contract).build();
    }

    @DELETE
    @Path("/{id}/contracts/{contractId}")
    @Transactional
    public Response deleteContract(@PathParam("id") Long id,
                                   @PathParam("contractId") Long contractId) {
        logger.info("契約削除。仕入先ID=" + id + ", 契約ID=" + contractId);
        SupplierContract contract = em.find(SupplierContract.class, contractId);
        if (contract == null || !contract.getSupplier().getId().equals(id)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        em.remove(contract);
        return Response.noContent().build();
    }

    @GET
    @Path("/{id}/contacts")
    @SuppressWarnings("unchecked")
    public Response getSupplierContacts(@PathParam("id") Long id) {
        logger.info("仕入先連絡先取得。仕入先ID=" + id);

        List<SupplierContact> contacts = em.createNamedQuery("SupplierContact.findBySupplier")
                .setParameter("supplierId", id)
                .getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (SupplierContact c : contacts) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", c.getId());
            map.put("supplierId", id);
            map.put("name", c.getLastName() + " " + c.getFirstName());
            map.put("department", c.getDepartment());
            map.put("position", "");
            map.put("phone", c.getPhone());
            map.put("email", c.getEmail());
            map.put("isPrimary", c.isPrimary());
            result.add(map);
        }

        return Response.ok(result).build();
    }

    /**
     * 仕入先の評価履歴を取得する。
     *
     * @param id 仕入先ID
     * @return 評価履歴
     */
    @GET
    @Path("/{id}/ratings")
    public Response getSupplierRatings(@PathParam("id") Long id) {
        logger.info("仕入先の評価取得。仕入先ID=" + id);

        List<SupplierRating> ratings = supplierService.getRatingHistory(id);
        return Response.ok(ratings).build();
    }

    @GET
    @Path("/{id}/certifications")
    @SuppressWarnings("unchecked")
    public Response getSupplierCertifications(@PathParam("id") Long id) {
        logger.info("仕入先の認証取得。仕入先ID=" + id);

        supplierService.findById(id);

        List<SupplierCertification> certs = em.createNamedQuery("SupplierCertification.findBySupplier")
                .setParameter("supplierId", id)
                .getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (SupplierCertification cert : certs) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", cert.getId());
            map.put("certType", cert.getCertType());
            map.put("certNumber", cert.getCertNumber());
            map.put("issuedDate", cert.getIssuedDate());
            map.put("expiryDate", cert.getExpiryDate());
            map.put("status", cert.getStatus());
            result.add(map);
        }

        return Response.ok(result).build();
    }

    @POST
    @Path("/{id}/certifications")
    @Transactional
    public Response createCertification(@PathParam("id") Long id, Map<String, Object> data) {
        logger.info("認証作成。仕入先ID=" + id);
        Supplier supplier = supplierService.findById(id);

        SupplierCertification cert = new SupplierCertification();
        cert.setSupplier(supplier);
        cert.setCertType(data.get("certType").toString());
        cert.setCertNumber(data.get("certNumber") != null ? data.get("certNumber").toString() : null);
        cert.setIssuedDate(parseDate(data.get("issuedDate")));
        cert.setExpiryDate(parseDate(data.get("expiryDate")));
        cert.setStatus(data.get("status") != null ? data.get("status").toString() : "ACTIVE");
        em.persist(cert);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", cert.getId());
        map.put("certType", cert.getCertType());
        map.put("certNumber", cert.getCertNumber());
        map.put("issuedDate", cert.getIssuedDate());
        map.put("expiryDate", cert.getExpiryDate());
        map.put("status", cert.getStatus());
        return Response.status(Response.Status.CREATED).entity(map).build();
    }

    @PUT
    @Path("/{id}/certifications/{certId}")
    @Transactional
    public Response updateCertification(@PathParam("id") Long id,
                                        @PathParam("certId") Long certId,
                                        Map<String, Object> data) {
        logger.info("認証更新。仕入先ID=" + id + ", 認証ID=" + certId);
        SupplierCertification cert = em.find(SupplierCertification.class, certId);
        if (cert == null || !cert.getSupplier().getId().equals(id)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if (data.containsKey("certType")) cert.setCertType(data.get("certType").toString());
        if (data.containsKey("certNumber")) cert.setCertNumber(data.get("certNumber") != null ? data.get("certNumber").toString() : null);
        if (data.containsKey("issuedDate")) cert.setIssuedDate(parseDate(data.get("issuedDate")));
        if (data.containsKey("expiryDate")) cert.setExpiryDate(parseDate(data.get("expiryDate")));
        if (data.containsKey("status")) cert.setStatus(data.get("status").toString());
        em.merge(cert);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", cert.getId());
        map.put("certType", cert.getCertType());
        map.put("certNumber", cert.getCertNumber());
        map.put("issuedDate", cert.getIssuedDate());
        map.put("expiryDate", cert.getExpiryDate());
        map.put("status", cert.getStatus());
        return Response.ok(map).build();
    }

    @DELETE
    @Path("/{id}/certifications/{certId}")
    @Transactional
    public Response deleteCertification(@PathParam("id") Long id,
                                        @PathParam("certId") Long certId) {
        logger.info("認証削除。仕入先ID=" + id + ", 認証ID=" + certId);
        SupplierCertification cert = em.find(SupplierCertification.class, certId);
        if (cert == null || !cert.getSupplier().getId().equals(id)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        em.remove(cert);
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/products")
    @Transactional
    public Response addSupplierProduct(@PathParam("id") Long id, Map<String, Object> data) {
        logger.info("製品紐付け追加。仕入先ID=" + id);
        Supplier supplier = supplierService.findById(id);

        Long productId = ((Number) data.get("productId")).longValue();
        com.proquip.ejb.entity.product.Product product = em.find(
                com.proquip.ejb.entity.product.Product.class, productId);
        if (product == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"製品が見つかりません。ID: " + productId + "\"}").build();
        }

        SupplierProduct sp = new SupplierProduct();
        sp.setSupplier(supplier);
        sp.setProduct(product);
        sp.setSupplierSku(data.get("supplierSku") != null ? data.get("supplierSku").toString() : null);
        sp.setUnitCost(data.get("unitCost") != null ? new BigDecimal(data.get("unitCost").toString()) : BigDecimal.ZERO);
        sp.setLeadTimeDays(data.get("leadTimeDays") != null ? ((Number) data.get("leadTimeDays")).intValue() : 0);
        sp.setMinOrderQty(data.get("minOrderQty") != null ? ((Number) data.get("minOrderQty")).intValue() : 1);
        sp.setPreferred(data.get("isPreferred") != null && Boolean.parseBoolean(data.get("isPreferred").toString()));
        em.persist(sp);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", sp.getId());
        map.put("productId", product.getId());
        map.put("productName", product.getName());
        map.put("productSku", product.getSku());
        map.put("supplierSku", sp.getSupplierSku());
        map.put("unitCost", sp.getUnitCost());
        map.put("leadTimeDays", sp.getLeadTimeDays());
        map.put("minOrderQty", sp.getMinOrderQty());
        map.put("isPreferred", sp.isPreferred());
        return Response.status(Response.Status.CREATED).entity(map).build();
    }

    @DELETE
    @Path("/{id}/products/{spId}")
    @Transactional
    public Response removeSupplierProduct(@PathParam("id") Long id,
                                          @PathParam("spId") Long spId) {
        logger.info("製品紐付け解除。仕入先ID=" + id + ", 紐付けID=" + spId);
        SupplierProduct sp = em.find(SupplierProduct.class, spId);
        if (sp == null || !sp.getSupplier().getId().equals(id)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        em.remove(sp);
        return Response.noContent().build();
    }

    /**
     * 仕入先に評価を登録する。
     *
     * @param id         仕入先ID
     * @param ratingData 評価データ（qualityScore, deliveryScore, priceScore, comments）
     * @param secCtx     セキュリティコンテキスト
     * @return 登録された評価
     */
    @POST
    @Path("/{id}/rate")
    public Response rateSupplier(@PathParam("id") Long id,
                                 Map<String, Object> ratingData,
                                 @Context SecurityContext secCtx) {
        logger.info("仕入先評価登録。仕入先ID=" + id
                + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        BigDecimal qualityScore = new BigDecimal(ratingData.get("qualityScore").toString());
        BigDecimal deliveryScore = new BigDecimal(ratingData.get("deliveryScore").toString());
        BigDecimal priceScore = new BigDecimal(ratingData.get("priceScore").toString());
        BigDecimal serviceScore = ratingData.get("serviceScore") != null
                ? new BigDecimal(ratingData.get("serviceScore").toString())
                : new BigDecimal("3.0");
        String comments = ratingData.get("comments") != null
                ? ratingData.get("comments").toString() : null;

        Long ratedBy = resolveUserId(secCtx);

        SupplierRating rating = supplierService.addRating(
                id, qualityScore, deliveryScore, priceScore, serviceScore, comments, ratedBy);

        return Response.status(Response.Status.CREATED).entity(rating).build();
    }

    /**
     * 複数の仕入先を比較する。
     *
     * @param supplierIds 比較対象の仕入先IDリスト（カンマ区切り）
     * @return 比較結果
     */
    @GET
    @Path("/compare")
    public Response compareSuppliers(@QueryParam("supplierIds") String supplierIds) {
        logger.info("仕入先比較。IDs=" + supplierIds);

        if (supplierIds == null || supplierIds.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"supplierIdsパラメータは必須です。\"}")
                    .build();
        }

        String[] idStrings = supplierIds.split(",");
        List<Map<String, Object>> comparisonResults = new ArrayList<>();

        for (int i = 0; i < idStrings.length; i++) {
            try {
                Long supplierId = Long.parseLong(idStrings[i].trim());
                Map<String, Object> report = supplierService.getPerformanceReport(supplierId);
                comparisonResults.add(report);
            } catch (NumberFormatException e) {
                logger.warning("不正な仕入先ID: " + idStrings[i]);
            } catch (Exception e) {
                logger.warning("仕入先情報の取得に失敗。ID=" + idStrings[i] + ": " + e.getMessage());
            }
        }

        return Response.ok(comparisonResults).build();
    }

    private SupplierDTO enrichDto(SupplierDTO dto, Supplier entity) {
        try {
            if (entity.getContacts() != null && !entity.getContacts().isEmpty()) {
                var primary = entity.getContacts().stream()
                        .filter(c -> c.isPrimary())
                        .findFirst()
                        .orElse(entity.getContacts().get(0));
                dto.setEmail(primary.getEmail() != null ? primary.getEmail() : "");
                dto.setPhone(primary.getPhone() != null ? primary.getPhone() : "");
            } else {
                dto.setEmail("");
                dto.setPhone("");
            }
        } catch (Exception e) {
            dto.setEmail("");
            dto.setPhone("");
        }

        try {
            if (entity.getRatings() != null && !entity.getRatings().isEmpty()) {
                BigDecimal sum = BigDecimal.ZERO;
                int count = 0;
                for (var r : entity.getRatings()) {
                    if (r.getOverallScore() != null) {
                        sum = sum.add(r.getOverallScore());
                        count++;
                    }
                }
                if (count > 0) {
                    dto.setRating(sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP));
                } else {
                    dto.setRating(BigDecimal.ZERO);
                }
            } else {
                dto.setRating(BigDecimal.ZERO);
            }
        } catch (Exception e) {
            dto.setRating(BigDecimal.ZERO);
        }

        return dto;
    }

    private Long resolveUserId(SecurityContext secCtx) {
        if (secCtx == null || secCtx.getUserPrincipal() == null) {
            return 1L;
        }
        String principalName = secCtx.getUserPrincipal().getName();

        List<UserProfile> users = em.createQuery(
                "SELECT u FROM UserProfile u WHERE u.keycloakId = :keycloakId", UserProfile.class)
                .setParameter("keycloakId", principalName)
                .getResultList();
        if (!users.isEmpty()) {
            return users.get(0).getId();
        }

        @SuppressWarnings("unchecked")
        List<Object> ids = em.createNativeQuery("SELECT id FROM user_profile WHERE username = ?1")
                .setParameter(1, principalName)
                .getResultList();
        if (!ids.isEmpty()) {
            return ((Number) ids.get(0)).longValue();
        }

        logger.warning("principalName=" + principalName + " に対応するUserProfileが見つかりません。デフォルトID=1Lを使用します。");
        return 1L;
    }

    private java.util.Date parseDate(Object value) {
        if (value == null) return null;
        try {
            return new java.text.SimpleDateFormat("yyyy-MM-dd").parse(value.toString().substring(0, 10));
        } catch (Exception e) {
            return null;
        }
    }
}
