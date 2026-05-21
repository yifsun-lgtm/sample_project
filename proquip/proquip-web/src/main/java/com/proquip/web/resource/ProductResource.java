package com.proquip.web.resource;

import com.proquip.common.constant.AppConstants;
import com.proquip.common.dto.PageResultDto;
import com.proquip.common.dto.ProductDTO;
import com.proquip.common.dto.ProductDetailDto;
import com.proquip.common.dto.SupplierDTO;
import com.proquip.ejb.mapper.ProductMapper;
import com.proquip.ejb.mapper.SupplierMapper;
import com.proquip.ejb.entity.product.Category;
import com.proquip.ejb.entity.product.Manufacturer;
import com.proquip.ejb.entity.product.Product;
import com.proquip.ejb.entity.product.ProductDocument;
import com.proquip.ejb.entity.product.ProductBundle;
import com.proquip.ejb.entity.product.ProductBundleItem;
import com.proquip.ejb.entity.product.ProductImage;
import com.proquip.ejb.entity.product.ProductChangeLog;
import com.proquip.ejb.entity.product.ProductSpecification;
import com.proquip.ejb.entity.product.UnitOfMeasure;
import com.proquip.ejb.entity.organization.UserProfile;
import com.proquip.ejb.entity.supplier.SupplierProduct;
import com.proquip.ejb.service.ImportExportServiceBean;
import com.proquip.ejb.service.ProductServiceBean;
import com.proquip.ejb.service.SupplierServiceBean;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
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

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;

import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 商品管理RESTリソース。
 *
 * <p>商品のCRUD操作、検索、カテゴリ管理、インポート/エクスポート機能を提供する。</p>
 *
 * <p>【技術的負債 #5 - 関心の混在】
 * カテゴリ管理のエンドポイント（/categories）がこのリソースに含まれているが、
 * 本来は別のCategoryResourceとして分離すべきである。</p>
 *
 * <p>【技術的負債 #5】
 * /search エンドポイントで {@link EntityManager} を直接注入し、
 * JPQLをインラインで構築している。サービス層をバイパスしており、
 * ビジネスロジックとプレゼンテーション層の分離が崩れている。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductResource {

    private static final Logger logger = Logger.getLogger(ProductResource.class.getName());

    @Inject
    private ProductServiceBean productService;

    @Inject
    private SupplierServiceBean supplierService;

    @Inject
    private ImportExportServiceBean importExportService;

    @Inject
    private ProductMapper productMapper;

    @Inject
    private SupplierMapper supplierMapper;

    /**
     * 技術的負債 #5: RESTリソースにEntityManagerを直接注入。
     * サービス層をバイパスしてDBアクセスを行っている。
     */
    @PersistenceContext
    private EntityManager em;

    // ========================================================================
    // 商品CRUD
    // ========================================================================

    /**
     * 商品一覧を取得する（ページネーション付き）。
     *
     * @param page       ページ番号（0始まり、デフォルト: 0）
     * @param size       ページサイズ（デフォルト: 20）
     * @param keyword    検索キーワード（商品名・説明の部分一致）
     * @param categoryId カテゴリIDによるフィルタ
     * @param status     ステータスによるフィルタ
     * @return 商品一覧のページネーション結果
     */
    @GET
    public Response listProducts(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("keyword") String keyword,
            @QueryParam("categoryId") Long categoryId,
            @QueryParam("status") String status,
            @QueryParam("manufacturerId") Long manufacturerId) {

        logger.info("商品一覧取得。page=" + page + ", size=" + size
                + ", keyword=" + keyword + ", categoryId=" + categoryId);

        if (size > AppConstants.MAX_PAGE_SIZE) {
            size = AppConstants.MAX_PAGE_SIZE;
        }

        List<Product> products = productService.searchProducts(
                keyword, categoryId, status, null, null);

        if (manufacturerId != null) {
            products = new ArrayList<>(products);
            products.removeIf(p -> p.getManufacturer() == null || !manufacturerId.equals(p.getManufacturer().getId()));
        }

        int totalCount = products.size();
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalCount);

        List<ProductDTO> dtoList = new ArrayList<>();
        if (fromIndex < totalCount) {
            List<Product> pageProducts = products.subList(fromIndex, toIndex);
            for (int i = 0; i < pageProducts.size(); i++) {
                ProductDTO dto = productMapper.toDto(pageProducts.get(i));
                try {
                    Object qty = em.createQuery(
                            "SELECT COALESCE(SUM(ii.quantity), 0) FROM InventoryItem ii WHERE ii.product.id = :pid")
                            .setParameter("pid", pageProducts.get(i).getId())
                            .getSingleResult();
                    dto.setStockQuantity(((Number) qty).intValue());
                } catch (Exception e) {
                    dto.setStockQuantity(0);
                }
                dtoList.add(dto);
            }
        }

        PageResultDto<ProductDTO> result = new PageResultDto<>(dtoList, totalCount, page, size);
        return Response.ok(result).build();
    }

    /**
     * 商品詳細を取得する。
     *
     * @param id 商品ID
     * @return 商品詳細DTO
     */
    @GET
    @Path("/{id}")
    @SuppressWarnings("unchecked")
    public Response getProduct(@PathParam("id") Long id) {
        logger.info("商品詳細取得。ID=" + id);

        List<Product> results = em.createQuery(
                "SELECT p FROM Product p " +
                "LEFT JOIN FETCH p.category " +
                "LEFT JOIN FETCH p.manufacturer " +
                "LEFT JOIN FETCH p.unit " +
                "WHERE p.id = :id", Product.class)
                .setParameter("id", id)
                .getResultList();

        if (results.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "商品が見つかりません。ID=" + id))
                    .build();
        }

        Product product = results.get(0);

        List<ProductSpecification> specs = em.createQuery(
                "SELECT ps FROM ProductSpecification ps WHERE ps.product.id = :id",
                ProductSpecification.class)
                .setParameter("id", id)
                .getResultList();
        product.setSpecifications(specs);

        List<ProductImage> images = em.createQuery(
                "SELECT pi FROM ProductImage pi WHERE pi.product.id = :id ORDER BY pi.sortOrder",
                ProductImage.class)
                .setParameter("id", id)
                .getResultList();
        product.setImages(images);

        List<ProductDocument> docs = em.createQuery(
                "SELECT pd FROM ProductDocument pd WHERE pd.product.id = :id",
                ProductDocument.class)
                .setParameter("id", id)
                .getResultList();
        product.setDocuments(docs);

        ProductDetailDto dto = productMapper.toDetailDto(product);

        // 仕様をFE期待のJSON文字列に変換
        if (product.getSpecifications() != null && !product.getSpecifications().isEmpty()) {
            JsonObjectBuilder builder = Json.createObjectBuilder();
            for (ProductSpecification spec : product.getSpecifications()) {
                String val = spec.getSpecValue() != null ? spec.getSpecValue() : "";
                if (spec.getSpecUnit() != null && !spec.getSpecUnit().isEmpty()) {
                    val += " " + spec.getSpecUnit();
                }
                builder.add(spec.getSpecName() != null ? spec.getSpecName() : "", val);
            }
            dto.setSpecifications(builder.build().toString());
        }

        // ドキュメントを手動マッピング
        if (product.getDocuments() != null && !product.getDocuments().isEmpty()) {
            List<ProductDetailDto.DocumentDto> docDtos = new ArrayList<>();
            for (ProductDocument doc : product.getDocuments()) {
                ProductDetailDto.DocumentDto docDto = new ProductDetailDto.DocumentDto();
                docDto.setId(doc.getId());
                docDto.setDocType(doc.getDocType());
                docDto.setFileName(doc.getFileName());
                docDto.setFilePath(doc.getFilePath());
                docDto.setDocVersion(doc.getDocVersion());
                docDtos.add(docDto);
            }
            dto.setDocuments(docDtos);
        }

        return Response.ok(dto).build();
    }

    /**
     * 商品を新規作成する。
     *
     * @param dto     商品DTO
     * @param secCtx  セキュリティコンテキスト
     * @return 作成された商品DTO
     */
    @POST
    public Response createProduct(ProductDTO dto, @Context SecurityContext secCtx) {
        logger.info("商品作成。SKU=" + dto.getSkuCode()
                + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        Product entity = productMapper.toEntity(dto);
        setRelations(entity, dto);
        saveSpecifications(entity, dto.getSpecifications());

        Product created = productService.createProduct(entity);

        Product fresh = em.createQuery(
                "SELECT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.manufacturer LEFT JOIN FETCH p.unit WHERE p.id = :id",
                Product.class)
                .setParameter("id", created.getId())
                .getSingleResult();
        ProductDTO resultDto = productMapper.toDto(fresh);

        return Response.status(Response.Status.CREATED).entity(resultDto).build();
    }

    /**
     * 商品を更新する。
     *
     * @param id      商品ID
     * @param dto     更新データ
     * @param secCtx  セキュリティコンテキスト
     * @return 更新後の商品DTO
     */
    @PUT
    @Path("/{id}")
    public Response updateProduct(@PathParam("id") Long id, ProductDTO dto,
                                  @Context SecurityContext secCtx) {
        logger.info("商品更新。ID=" + id + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        Product existing = em.find(Product.class, id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "商品が見つかりません。ID=" + id))
                    .build();
        }

        Product entity = productMapper.toEntity(dto);
        entity.setId(id);
        setRelations(entity, dto);

        // 既存の仕様を削除して再作成
        em.createQuery("DELETE FROM ProductSpecification ps WHERE ps.product.id = :id")
                .setParameter("id", id)
                .executeUpdate();
        saveSpecifications(entity, dto.getSpecifications());

        Product updated = productService.updateProduct(entity);

        Product fresh = em.createQuery(
                "SELECT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.manufacturer LEFT JOIN FETCH p.unit WHERE p.id = :id",
                Product.class)
                .setParameter("id", updated.getId())
                .getSingleResult();
        ProductDTO resultDto = productMapper.toDto(fresh);

        return Response.ok(resultDto).build();
    }

    /**
     * 商品を論理削除する。
     *
     * @param id     商品ID
     * @param secCtx セキュリティコンテキスト
     * @return 204 No Content
     */
    @DELETE
    @Path("/{id}")
    public Response deleteProduct(@PathParam("id") Long id, @Context SecurityContext secCtx) {
        logger.info("商品削除（論理）。ID=" + id + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        productService.changeStatus(id, "DISCONTINUED");
        return Response.noContent().build();
    }

    /**
     * 商品の代替品を取得する。
     *
     * @param id 商品ID
     * @return 代替品リスト
     */
    @GET
    @Path("/{id}/alternatives")
    public Response getAlternatives(@PathParam("id") Long id) {
        logger.info("代替品取得。商品ID=" + id);

        Product product = productService.findById(id);
        ProductDetailDto detailDto = productMapper.toDetailDto(product);
        List<Long> alternativeIds = detailDto.getAlternatives();

        List<ProductDTO> alternatives = new ArrayList<>();
        if (alternativeIds != null) {
            for (int i = 0; i < alternativeIds.size(); i++) {
                try {
                    Product alt = productService.findById(alternativeIds.get(i));
                    alternatives.add(productMapper.toDto(alt));
                } catch (Exception e) {
                    // 技術的負債 #7: 代替品が見つからない場合は無視
                    logger.warning("代替品が見つかりません。ID=" + alternativeIds.get(i));
                }
            }
        }

        return Response.ok(alternatives).build();
    }

    /**
     * 商品に紐づく仕入先を取得する。
     *
     * @param id 商品ID
     * @return 仕入先リスト
     */
    @GET
    @Path("/{id}/suppliers")
    @SuppressWarnings("unchecked")
    public Response getProductSuppliers(@PathParam("id") Long id) {
        logger.info("商品の仕入先取得。商品ID=" + id);

        // 技術的負債: サービス層にメソッドがないため、EntityManagerで直接問い合わせ
        List<SupplierProduct> supplierProducts = em.createQuery(
                "SELECT sp FROM SupplierProduct sp LEFT JOIN FETCH sp.supplier WHERE sp.product.id = :productId " +
                "ORDER BY sp.supplier.rating DESC")
                .setParameter("productId", id)
                .getResultList();

        List<SupplierDTO> suppliers = new ArrayList<>();
        for (int i = 0; i < supplierProducts.size(); i++) {
            suppliers.add(supplierMapper.toDto(supplierProducts.get(i).getSupplier()));
        }

        return Response.ok(suppliers).build();
    }

    @GET
    @Path("/check-sku")
    public Response checkSku(@QueryParam("sku") String sku) {
        logger.info("SKU重複チェック。SKU=" + sku);

        if (sku == null || sku.trim().isEmpty()) {
            return Response.ok(false).build();
        }

        Product existing = productService.findBySku(sku.trim());
        return Response.ok(existing != null).build();
    }

    // ========================================================================
    // カテゴリ管理（技術的負債 #5: 本来は別リソースにすべき）
    // ========================================================================

    /**
     * カテゴリ一覧を取得する。
     *
     * <p>技術的負債 #5: 本来は CategoryResource として分離すべき。</p>
     *
     * @return カテゴリ一覧
     */
    @GET
    @Path("/categories")
    @SuppressWarnings("unchecked")
    public Response listCategories() {
        logger.info("カテゴリ一覧取得。");

        List<Category> categories = em.createQuery(
                "SELECT c FROM Category c LEFT JOIN FETCH c.parent ORDER BY c.name",
                Category.class)
                .getResultList();

        Map<Long, Long> productCounts = new HashMap<>();
        List<Object[]> counts = em.createQuery(
                "SELECT p.category.id, COUNT(p) FROM Product p WHERE p.category IS NOT NULL GROUP BY p.category.id")
                .getResultList();
        for (Object[] row : counts) {
            productCounts.put((Long) row[0], (Long) row[1]);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < categories.size(); i++) {
            Category c = categories.get(i);
            Map<String, Object> catMap = new LinkedHashMap<>();
            catMap.put("id", c.getId());
            catMap.put("code", c.getCode());
            catMap.put("name", c.getName());
            catMap.put("description", c.getDescription() != null ? c.getDescription() : "");
            catMap.put("parentId", c.getParent() != null ? c.getParent().getId() : null);
            catMap.put("productCount", productCounts.getOrDefault(c.getId(), 0L));
            result.add(catMap);
        }

        return Response.ok(result).build();
    }

    /**
     * カテゴリを新規作成する。
     *
     * <p>技術的負債 #5: 本来は CategoryResource として分離すべき。</p>
     *
     * @param categoryData カテゴリ情報のMap
     * @return 作成されたカテゴリ情報
     */
    @POST
    @Path("/categories")
    @Transactional
    public Response createCategory(Map<String, Object> categoryData) {
        String name = (String) categoryData.get("name");
        logger.info("カテゴリ作成。name=" + name);

        Category category = new Category();
        category.setName(name);
        category.setCode("CAT-" + System.currentTimeMillis());
        category.setDescription((String) categoryData.get("description"));

        Object parentIdObj = categoryData.get("parentId");
        if (parentIdObj != null) {
            Long parentId = ((Number) parentIdObj).longValue();
            Category parent = em.find(Category.class, parentId);
            if (parent != null) {
                category.setParent(parent);
                category.setLevel(parent.getLevel() + 1);
            } else {
                category.setLevel(0);
            }
        } else {
            category.setLevel(0);
        }

        em.persist(category);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", category.getId());
        result.put("code", category.getCode());
        result.put("name", category.getName());
        result.put("description", category.getDescription() != null ? category.getDescription() : "");
        result.put("parentId", category.getParent() != null ? category.getParent().getId() : null);
        result.put("productCount", 0);

        return Response.status(Response.Status.CREATED).entity(result).build();
    }

    @PUT
    @Path("/categories/{id}")
    @Transactional
    public Response updateCategory(@PathParam("id") Long id, Map<String, Object> categoryData) {
        logger.info("カテゴリ更新。ID=" + id);

        Category category = em.find(Category.class, id);
        if (category == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "カテゴリが見つかりません。"))
                    .build();
        }

        if (categoryData.containsKey("name")) {
            category.setName((String) categoryData.get("name"));
        }
        if (categoryData.containsKey("description")) {
            category.setDescription((String) categoryData.get("description"));
        }

        Object parentIdObj = categoryData.get("parentId");
        if (parentIdObj != null) {
            Long parentId = ((Number) parentIdObj).longValue();
            Category parent = em.find(Category.class, parentId);
            category.setParent(parent);
            category.setLevel(parent != null ? parent.getLevel() + 1 : 0);
        } else {
            category.setParent(null);
            category.setLevel(0);
        }

        em.merge(category);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", category.getId());
        result.put("name", category.getName());
        result.put("description", category.getDescription() != null ? category.getDescription() : "");
        result.put("parentId", category.getParent() != null ? category.getParent().getId() : null);

        return Response.ok(result).build();
    }

    @DELETE
    @Path("/categories/{id}")
    @Transactional
    public Response deleteCategory(@PathParam("id") Long id) {
        logger.info("カテゴリ削除。ID=" + id);

        Category category = em.find(Category.class, id);
        if (category == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "カテゴリが見つかりません。"))
                    .build();
        }

        Long childCount = em.createQuery("SELECT COUNT(c) FROM Category c WHERE c.parent.id = :id", Long.class)
                .setParameter("id", id).getSingleResult();
        if (childCount > 0) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "子カテゴリが存在するため削除できません。"))
                    .build();
        }

        Long productCount = em.createQuery("SELECT COUNT(p) FROM Product p WHERE p.category.id = :id", Long.class)
                .setParameter("id", id).getSingleResult();
        if (productCount > 0) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "このカテゴリに紐付く製品が存在するため削除できません。"))
                    .build();
        }

        em.remove(category);
        return Response.noContent().build();
    }

    // ========================================================================
    // メーカー管理
    // ========================================================================

    @GET
    @Path("/manufacturers")
    @SuppressWarnings("unchecked")
    public Response listManufacturers() {
        logger.info("メーカー一覧取得。");

        List<Manufacturer> manufacturers = em.createQuery(
                "SELECT m FROM Manufacturer m ORDER BY m.name",
                Manufacturer.class)
                .getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Manufacturer m : manufacturers) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", m.getId());
            map.put("code", m.getCode());
            map.put("name", m.getName());
            map.put("country", m.getCountry());
            map.put("website", m.getWebsite());
            result.add(map);
        }

        return Response.ok(result).build();
    }

    // ========================================================================
    // 変更履歴
    // ========================================================================

    @GET
    @Path("/{id}/change-log")
    public Response getChangeLog(@PathParam("id") Long id) {
        logger.info("製品変更履歴取得: productId=" + id);

        Product product = em.find(Product.class, id);
        if (product == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        List<ProductChangeLog> logs = em.createQuery(
                "SELECT cl FROM ProductChangeLog cl WHERE cl.product.id = :productId ORDER BY cl.createdAt DESC",
                ProductChangeLog.class)
                .setParameter("productId", id)
                .getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (ProductChangeLog cl : logs) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", cl.getId());
            map.put("changeType", cl.getChangeType());
            map.put("field", cl.getFieldName());
            map.put("oldValue", cl.getOldValue());
            map.put("newValue", cl.getNewValue());
            map.put("changedAt", cl.getCreatedAt());

            String changedByName = null;
            if (cl.getChangedBy() != null) {
                UserProfile user = em.find(UserProfile.class, cl.getChangedBy());
                if (user != null) {
                    changedByName = user.getLastName() + user.getFirstName();
                }
            }
            map.put("changedBy", changedByName != null ? changedByName : "-");

            result.add(map);
        }

        return Response.ok(result).build();
    }

    // ========================================================================
    // バンドル管理
    // ========================================================================

    @GET
    @Path("/bundles")
    @SuppressWarnings("unchecked")
    @Transactional
    public Response listBundles() {
        logger.info("バンドル一覧取得。");

        List<ProductBundle> bundles = em.createQuery(
                "SELECT DISTINCT b FROM ProductBundle b LEFT JOIN FETCH b.bundleItems ORDER BY b.bundleName",
                ProductBundle.class)
                .getResultList();

        for (ProductBundle b : bundles) {
            if (b.getBundleItems() != null) {
                for (ProductBundleItem bi : b.getBundleItems()) {
                    bi.getProduct().getName();
                }
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (ProductBundle b : bundles) {
            result.add(toBundleMap(b));
        }

        return Response.ok(result).build();
    }

    @POST
    @Path("/bundles")
    @Transactional
    public Response createBundle(Map<String, Object> data) {
        logger.info("バンドル作成。name=" + data.get("name"));

        ProductBundle bundle = new ProductBundle();
        bundle.setBundleCode("BDL-" + System.currentTimeMillis());
        bundle.setBundleName((String) data.get("name"));
        bundle.setDescription((String) data.get("description"));
        bundle.setStatus(data.get("status") != null ? (String) data.get("status") : "ACTIVE");
        bundle.setCreatedBy("system");
        bundle.setUpdatedBy("system");

        if (data.get("discountPercentage") != null) {
            bundle.setDiscount(new BigDecimal(data.get("discountPercentage").toString()));
        } else {
            bundle.setDiscount(BigDecimal.ZERO);
        }

        em.persist(bundle);
        em.flush();

        addBundleItems(bundle, data);

        return Response.status(Response.Status.CREATED).entity(toBundleMap(bundle)).build();
    }

    @PUT
    @Path("/bundles/{id}")
    @Transactional
    public Response updateBundle(@PathParam("id") Long id, Map<String, Object> data) {
        logger.info("バンドル更新。ID=" + id);

        ProductBundle bundle = em.find(ProductBundle.class, id);
        if (bundle == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "バンドルが見つかりません。"))
                    .build();
        }

        bundle.setBundleName((String) data.get("name"));
        bundle.setDescription((String) data.get("description"));
        bundle.setStatus(data.get("status") != null ? (String) data.get("status") : bundle.getStatus());
        bundle.setUpdatedBy("system");

        if (data.get("discountPercentage") != null) {
            bundle.setDiscount(new BigDecimal(data.get("discountPercentage").toString()));
        }

        bundle.getBundleItems().clear();
        em.flush();

        addBundleItems(bundle, data);

        return Response.ok(toBundleMap(bundle)).build();
    }

    @DELETE
    @Path("/bundles/{id}")
    @Transactional
    public Response deleteBundle(@PathParam("id") Long id) {
        logger.info("バンドル削除。ID=" + id);

        ProductBundle bundle = em.find(ProductBundle.class, id);
        if (bundle == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "バンドルが見つかりません。"))
                    .build();
        }

        em.remove(bundle);
        return Response.noContent().build();
    }

    @SuppressWarnings("unchecked")
    private void addBundleItems(ProductBundle bundle, Map<String, Object> data) {
        List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
        BigDecimal totalPrice = BigDecimal.ZERO;
        if (items != null) {
            int order = 0;
            for (Map<String, Object> item : items) {
                Long productId = ((Number) item.get("productId")).longValue();
                int quantity = ((Number) item.get("quantity")).intValue();
                Product product = em.find(Product.class, productId);
                if (product != null) {
                    ProductBundleItem bi = new ProductBundleItem();
                    bi.setBundle(bundle);
                    bi.setProduct(product);
                    bi.setQuantity(quantity);
                    bundle.getBundleItems().add(bi);
                    em.persist(bi);
                    if (product.getUnitPrice() != null) {
                        totalPrice = totalPrice.add(product.getUnitPrice().multiply(BigDecimal.valueOf(quantity)));
                    }
                }
            }
        }
        BigDecimal discountPct = bundle.getDiscount() != null ? bundle.getDiscount() : BigDecimal.ZERO;
        BigDecimal discountAmount = totalPrice.multiply(discountPct).divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.FLOOR);
        bundle.setBundlePrice(totalPrice.subtract(discountAmount));
    }

    private Map<String, Object> toBundleMap(ProductBundle b) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", b.getId());
        map.put("name", b.getBundleName());
        map.put("description", b.getDescription() != null ? b.getDescription() : "");
        map.put("status", b.getStatus() != null ? b.getStatus() : "ACTIVE");
        map.put("discountPercentage", b.getDiscount() != null ? b.getDiscount().doubleValue() : 0);
        map.put("bundlePrice", b.getBundlePrice() != null ? b.getBundlePrice().doubleValue() : 0);
        map.put("createdAt", b.getCreatedAt() != null ? b.getCreatedAt().toString() : "");

        BigDecimal total = BigDecimal.ZERO;
        List<Map<String, Object>> products = new ArrayList<>();
        if (b.getBundleItems() != null) {
            for (ProductBundleItem bi : b.getBundleItems()) {
                Map<String, Object> itemMap = new LinkedHashMap<>();
                Product p = bi.getProduct();
                itemMap.put("productId", p.getId());
                itemMap.put("productName", p.getName());
                itemMap.put("productSku", p.getSku());
                itemMap.put("unitPrice", p.getUnitPrice() != null ? p.getUnitPrice().doubleValue() : 0);
                itemMap.put("quantity", bi.getQuantity());
                double subtotal = (p.getUnitPrice() != null ? p.getUnitPrice().doubleValue() : 0) * bi.getQuantity();
                itemMap.put("subtotal", subtotal);
                products.add(itemMap);
                total = total.add(BigDecimal.valueOf(subtotal));
            }
        }
        map.put("products", products);
        map.put("totalPrice", total.doubleValue());

        return map;
    }

    // ========================================================================
    // 検索（技術的負債 #5: サービス層をバイパス）
    // ========================================================================

    /**
     * 商品を詳細検索する。
     *
     * <p>【技術的負債 #5】EntityManagerを直接注入し、JPQLをインラインで構築している。
     * ProductServiceBean.searchProducts() を使用すべきだが、追加の検索条件
     * （価格帯、製造元）を使うためにバイパスしている。</p>
     *
     * @param keyword        キーワード
     * @param categoryId     カテゴリID
     * @param status         ステータス
     * @param minPrice       最低価格
     * @param maxPrice       最高価格
     * @param manufacturerId 製造元ID
     * @param page           ページ番号
     * @param size           ページサイズ
     * @return 検索結果
     */
    @GET
    @Path("/search")
    @SuppressWarnings("unchecked")
    public Response searchProducts(
            @QueryParam("keyword") String keyword,
            @QueryParam("categoryId") Long categoryId,
            @QueryParam("status") String status,
            @QueryParam("minPrice") BigDecimal minPrice,
            @QueryParam("maxPrice") BigDecimal maxPrice,
            @QueryParam("manufacturerId") Long manufacturerId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {

        logger.info("商品検索。keyword=" + keyword + ", categoryId=" + categoryId
                + ", minPrice=" + minPrice + ", maxPrice=" + maxPrice);

        // 技術的負債 #5: EntityManagerで直接JPQL構築（サービス層をバイパス）
        StringBuffer jpql = new StringBuffer();
        jpql.append("SELECT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.manufacturer LEFT JOIN FETCH p.unit WHERE 1=1");

        StringBuffer countJpql = new StringBuffer();
        countJpql.append("SELECT COUNT(p) FROM Product p WHERE 1=1");

        String whereClause = "";

        if (keyword != null && !keyword.trim().isEmpty()) {
            whereClause += " AND (p.name LIKE :keyword OR p.description LIKE :keyword OR p.sku LIKE :keyword)";
        }
        if (categoryId != null) {
            whereClause += " AND p.category.id = :categoryId";
        }
        if (status != null && !status.isEmpty()) {
            whereClause += " AND p.status = :status";
        }
        if (minPrice != null) {
            whereClause += " AND p.unitPrice >= :minPrice";
        }
        if (maxPrice != null) {
            whereClause += " AND p.unitPrice <= :maxPrice";
        }
        if (manufacturerId != null) {
            whereClause += " AND p.manufacturer.id = :manufacturerId";
        }

        jpql.append(whereClause);
        countJpql.append(whereClause);
        jpql.append(" ORDER BY p.name ASC");

        // データクエリ
        Query dataQuery = em.createQuery(jpql.toString());
        Query countQuery = em.createQuery(countJpql.toString());

        if (keyword != null && !keyword.trim().isEmpty()) {
            dataQuery.setParameter("keyword", "%" + keyword.trim() + "%");
            countQuery.setParameter("keyword", "%" + keyword.trim() + "%");
        }
        if (categoryId != null) {
            dataQuery.setParameter("categoryId", categoryId);
            countQuery.setParameter("categoryId", categoryId);
        }
        if (status != null && !status.isEmpty()) {
            dataQuery.setParameter("status", status);
            countQuery.setParameter("status", status);
        }
        if (minPrice != null) {
            dataQuery.setParameter("minPrice", minPrice);
            countQuery.setParameter("minPrice", minPrice);
        }
        if (maxPrice != null) {
            dataQuery.setParameter("maxPrice", maxPrice);
            countQuery.setParameter("maxPrice", maxPrice);
        }
        if (manufacturerId != null) {
            dataQuery.setParameter("manufacturerId", manufacturerId);
            countQuery.setParameter("manufacturerId", manufacturerId);
        }

        if (size > AppConstants.MAX_PAGE_SIZE) {
            size = AppConstants.MAX_PAGE_SIZE;
        }
        dataQuery.setFirstResult(page * size);
        dataQuery.setMaxResults(size);

        List<Product> products = dataQuery.getResultList();
        long totalCount = (Long) countQuery.getSingleResult();

        List<ProductDTO> dtoList = new ArrayList<>();
        for (int i = 0; i < products.size(); i++) {
            dtoList.add(productMapper.toDto(products.get(i)));
        }

        PageResultDto<ProductDTO> result = new PageResultDto<>(dtoList, totalCount, page, size);
        return Response.ok(result).build();
    }

    // ========================================================================
    // インポート/エクスポート
    // ========================================================================

    /**
     * CSV形式で商品をインポートする。
     *
     * @param inputStream CSVファイルの入力ストリーム
     * @return インポート結果
     */
    @POST
    @Path("/import")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response importProducts(InputStream inputStream) {
        logger.info("商品インポート開始。");

        try {
            // 技術的負債: InputStream を全てメモリ上に読み込んでいる
            byte[] data = inputStream.readAllBytes();
            String csvContent = new String(data, "UTF-8");
            String[] lines = csvContent.split("\n");

            List<Map<String, String>> productDataList = new ArrayList<>();
            // 1行目はヘッダーとしてスキップ
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] fields = line.split(",");
                Map<String, String> row = new HashMap<>();
                if (fields.length > 0) row.put("sku", fields[0].trim());
                if (fields.length > 1) row.put("name", fields[1].trim());
                if (fields.length > 2) row.put("unitPrice", fields[2].trim());
                if (fields.length > 3) row.put("categoryCode", fields[3].trim());
                productDataList.add(row);
            }

            Map<String, Object> result = productService.importProducts(productDataList);
            return Response.ok(result).build();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "商品インポートエラー。", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "インポートに失敗しました: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }
    }

    /**
     * 商品一覧をCSV形式でエクスポートする。
     *
     * @return CSV形式のレスポンス
     */
    @GET
    @Path("/export")
    @Produces("text/csv")
    public Response exportProducts() {
        logger.info("商品エクスポート開始。");

        List<Product> products = productService.findActiveProducts();
        String csv = productService.exportProductsToCsv(products);

        return Response.ok(csv)
                .header("Content-Disposition", "attachment; filename=\"products.csv\"")
                .build();
    }

    // ========================================================================
    // ドキュメントダウンロード
    // ========================================================================

    @GET
    @Path("/{productId}/documents/{docId}/download")
    public Response downloadDocument(@PathParam("productId") Long productId,
                                     @PathParam("docId") Long docId) {
        logger.info("ドキュメントダウンロード。商品ID=" + productId + ", ドキュメントID=" + docId);

        ProductDocument doc = em.find(ProductDocument.class, docId);
        if (doc == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "ドキュメントが見つかりません。"))
                    .build();
        }

        if (doc.getFilePath() != null && !doc.getFilePath().isEmpty()) {
            return Response.temporaryRedirect(URI.create(doc.getFilePath())).build();
        }

        return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "ファイルパスが設定されていません。"))
                .build();
    }

    // ========================================================================
    // 画像・ドキュメントアップロード
    // ========================================================================

    @POST
    @Path("/{id}/images")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public Response uploadImage(@PathParam("id") Long id, MultipartFormDataInput input) {
        Product product = em.find(Product.class, id);
        if (product == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "商品が見つかりません。"))
                    .build();
        }

        try {
            Map<String, List<InputPart>> formParts = input.getFormDataMap();

            List<InputPart> fileParts = formParts.get("file");
            if (fileParts == null || fileParts.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "ファイルが指定されていません。"))
                        .build();
            }
            InputStream fileStream = fileParts.get(0).getBody(InputStream.class, null);

            String fileName = formParts.containsKey("fileName")
                    ? formParts.get("fileName").get(0).getBodyAsString().trim() : "image";
            boolean isPrimary = formParts.containsKey("isPrimary")
                    && "true".equalsIgnoreCase(formParts.get("isPrimary").get(0).getBodyAsString().trim());

            logger.info("画像アップロード。商品ID=" + id + ", fileName=" + fileName);

            String basePath = System.getenv("UPLOAD_BASE_PATH");
            if (basePath == null) basePath = "/opt/jboss/wildfly/static-files";

            String ext = "";
            if (fileName.contains(".")) {
                ext = fileName.substring(fileName.lastIndexOf("."));
            }
            String storedName = "product-" + id + "-" + java.util.UUID.randomUUID().toString().substring(0, 8) + ext;
            String dirPath = basePath + "/images/products";
            java.io.File dir = new java.io.File(dirPath);
            if (!dir.exists()) dir.mkdirs();

            java.io.File target = new java.io.File(dir, storedName);
            try (java.io.OutputStream os = new java.io.FileOutputStream(target)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = fileStream.read(buf)) != -1) {
                    os.write(buf, 0, len);
                }
            }

            String imageUrl = "/images/products/" + storedName;

            ProductImage img = new ProductImage();
            img.setFileName(imageUrl);
            img.setOriginalFileName(fileName);
            img.setDiskPath(imageUrl);
            img.setMimeType("PHOTO");
            img.setPrimary(isPrimary);
            img.setSortOrder(1);
            img.setProduct(product);
            em.persist(img);

            Map<String, Object> result = new HashMap<>();
            result.put("id", img.getId());
            result.put("url", imageUrl);
            result.put("isPrimary", isPrimary);

            return Response.status(Response.Status.CREATED).entity(result).build();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "画像アップロードエラー。", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "画像のアップロードに失敗しました: " + e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/{id}/images/{imageId}")
    @Transactional
    public Response deleteImage(@PathParam("id") Long id, @PathParam("imageId") Long imageId) {
        logger.info("画像削除。商品ID=" + id + ", 画像ID=" + imageId);

        ProductImage img = em.find(ProductImage.class, imageId);
        if (img == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "画像が見つかりません。"))
                    .build();
        }

        String basePath = System.getenv("UPLOAD_BASE_PATH");
        if (basePath == null) basePath = "/opt/jboss/wildfly/static-files";
        java.io.File file = new java.io.File(basePath + img.getFileName());
        if (file.exists()) file.delete();

        em.remove(img);
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/documents")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public Response uploadDocument(@PathParam("id") Long id, MultipartFormDataInput input) {
        Product product = em.find(Product.class, id);
        if (product == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "商品が見つかりません。"))
                    .build();
        }

        try {
            Map<String, List<InputPart>> formParts = input.getFormDataMap();

            List<InputPart> fileParts = formParts.get("file");
            if (fileParts == null || fileParts.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "ファイルが指定されていません。"))
                        .build();
            }
            InputStream docStream = fileParts.get(0).getBody(InputStream.class, null);

            String fileName = formParts.containsKey("fileName")
                    ? formParts.get("fileName").get(0).getBodyAsString().trim() : "document";
            String docType = formParts.containsKey("docType")
                    ? formParts.get("docType").get(0).getBodyAsString().trim() : "DATASHEET";

            logger.info("ドキュメントアップロード。商品ID=" + id + ", fileName=" + fileName);

            String basePath = System.getenv("UPLOAD_BASE_PATH");
            if (basePath == null) basePath = "/opt/jboss/wildfly/static-files";

            String storedName = java.util.UUID.randomUUID().toString().substring(0, 8) + "-" + fileName;
            String dirPath = basePath + "/documents/products";
            java.io.File dir = new java.io.File(dirPath);
            if (!dir.exists()) dir.mkdirs();

            java.io.File target = new java.io.File(dir, storedName);
            try (java.io.OutputStream os = new java.io.FileOutputStream(target)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = docStream.read(buf)) != -1) {
                    os.write(buf, 0, len);
                }
            }

            String fileUrl = "/documents/products/" + storedName;

            ProductDocument doc = new ProductDocument();
            doc.setDocType(docType);
            doc.setFileName(fileName);
            doc.setFilePath(fileUrl);
            doc.setDocVersion("1.0");
            doc.setProduct(product);
            doc.setCreatedBy("system");
            doc.setUpdatedBy("system");
            em.persist(doc);

            Map<String, Object> result = new HashMap<>();
            result.put("id", doc.getId());
            result.put("fileName", doc.getFileName());
            result.put("filePath", fileUrl);
            result.put("docType", docType);

            return Response.status(Response.Status.CREATED).entity(result).build();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "ドキュメントアップロードエラー。", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "ドキュメントのアップロードに失敗しました: " + e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/{id}/documents/{docId}")
    @Transactional
    public Response deleteDocument2(@PathParam("id") Long id, @PathParam("docId") Long docId) {
        logger.info("ドキュメント削除。商品ID=" + id + ", ドキュメントID=" + docId);

        ProductDocument doc = em.find(ProductDocument.class, docId);
        if (doc == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "ドキュメントが見つかりません。"))
                    .build();
        }

        String basePath = System.getenv("UPLOAD_BASE_PATH");
        if (basePath == null) basePath = "/opt/jboss/wildfly/static-files";
        java.io.File file = new java.io.File(basePath + doc.getFilePath());
        if (file.exists()) file.delete();

        em.remove(doc);
        return Response.noContent().build();
    }

    // ========================================================================
    // プライベートヘルパー
    // ========================================================================

    @SuppressWarnings("unchecked")
    private void setRelations(Product entity, ProductDTO dto) {
        if (dto.getCategoryId() != null) {
            Category cat = em.find(Category.class, dto.getCategoryId());
            entity.setCategory(cat);
        }
        if (dto.getManufacturerId() != null) {
            Manufacturer mfr = em.find(Manufacturer.class, dto.getManufacturerId());
            entity.setManufacturer(mfr);
        }
        if (dto.getUnit() != null && !dto.getUnit().isEmpty()) {
            List<UnitOfMeasure> units = em.createQuery(
                    "SELECT u FROM UnitOfMeasure u WHERE u.name = :name", UnitOfMeasure.class)
                    .setParameter("name", dto.getUnit())
                    .getResultList();
            if (!units.isEmpty()) {
                entity.setUnit(units.get(0));
            }
        }
    }

    private void saveSpecifications(Product entity, String specsJson) {
        if (specsJson == null || specsJson.isEmpty() || specsJson.equals("{}")) {
            return;
        }
        try {
            jakarta.json.JsonObject json = jakarta.json.Json.createReader(
                    new java.io.StringReader(specsJson)).readObject();
            int order = 1;
            for (String key : json.keySet()) {
                if (key.isEmpty()) continue;
                ProductSpecification spec = new ProductSpecification();
                spec.setProduct(entity);
                spec.setSpecName(key);
                spec.setSpecValue(json.getString(key, ""));
                spec.setDisplayOrder(order++);
                entity.getSpecifications().add(spec);
            }
        } catch (Exception e) {
            logger.warning("仕様JSONパースエラー: " + e.getMessage());
        }
    }
}
