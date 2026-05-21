package com.proquip.web.resource;

import com.proquip.common.dto.BudgetDto;
import com.proquip.ejb.mapper.BudgetMapper;
import com.proquip.ejb.entity.organization.Department;
import com.proquip.ejb.entity.pricing.Budget;
import com.proquip.ejb.service.BudgetServiceBean;

import jakarta.inject.Inject;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * 予算管理RESTリソース。
 *
 * <p>予算のCRUD操作、利用可能額チェック、利用率表示、年度クローズを提供する。</p>
 *
 * <p>【技術的負債 #12】
 * 一部のエンドポイントで {@link HashMap} を返しており、型安全なDTOを使用していない。
 * API応答の型が不安定で、フロントエンド側の型定義と乖離するリスクがある。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
@Path("/budgets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BudgetResource {

    private static final Logger logger = Logger.getLogger(BudgetResource.class.getName());

    @Inject
    private BudgetServiceBean budgetService;

    @PersistenceContext
    private EntityManager em;

    private final BudgetMapper budgetMapper = new BudgetMapper();

    // ========================================================================
    // CRUD
    // ========================================================================

    /**
     * 予算一覧を取得する。
     *
     * <p>会計年度で絞り込み、さらに部門IDが指定されていれば
     * 該当部門の予算のみを返す。</p>
     *
     * @param fiscalYear  会計年度フィルタ
     * @param departmentId 部門IDフィルタ
     * @return 予算一覧
     */
    @GET
    public Response listBudgets(
            @QueryParam("fiscalYear") Integer fiscalYear,
            @QueryParam("departmentId") Long departmentId) {

        logger.info("予算一覧取得。fiscalYear=" + fiscalYear + ", departmentId=" + departmentId);

        // 部門IDと年度が両方指定された場合は単一予算を返す
        if (departmentId != null && fiscalYear != null) {
            Budget single = budgetService.findByDepartmentAndYear(departmentId, fiscalYear);
            List<BudgetDto> dtoList = new ArrayList<>();
            if (single != null) {
                dtoList.add(budgetMapper.toDto(single));
            }
            resolveDepartmentNames(dtoList);
            return Response.ok(dtoList).build();
        }

        // 年度のみの場合は年度で検索
        List<Budget> budgets = budgetService.findByFiscalYear(fiscalYear);

        List<BudgetDto> dtoList = new ArrayList<>();
        for (int i = 0; i < budgets.size(); i++) {
            dtoList.add(budgetMapper.toDto(budgets.get(i)));
        }
        resolveDepartmentNames(dtoList);

        return Response.ok(dtoList).build();
    }

    /**
     * 予算詳細を取得する。
     *
     * @param id 予算ID
     * @return 予算DTO
     */
    @GET
    @Path("/{id}")
    public Response getBudget(@PathParam("id") Long id) {
        logger.info("予算詳細取得。ID=" + id);

        Budget budget = budgetService.findById(id);
        BudgetDto dto = budgetMapper.toDto(budget);
        if (dto.getDepartmentId() != null) {
            Department dept = em.find(Department.class, dto.getDepartmentId());
            if (dept != null) {
                dto.setDepartmentName(dept.getName());
            }
        }

        return Response.ok(dto).build();
    }

    /**
     * 予算を新規作成する。
     *
     * @param dto    予算DTO
     * @param secCtx セキュリティコンテキスト
     * @return 作成された予算DTO
     */
    @POST
    public Response createBudget(BudgetDto dto, @Context SecurityContext secCtx) {
        logger.info("予算作成。部門ID=" + dto.getDepartmentId()
                + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        Budget entity = budgetMapper.toEntity(dto);
        Budget created = budgetService.createBudget(entity);
        BudgetDto resultDto = budgetMapper.toDto(created);

        return Response.status(Response.Status.CREATED).entity(resultDto).build();
    }

    /**
     * 予算を更新する。
     *
     * @param id     予算ID
     * @param dto    更新データ
     * @param secCtx セキュリティコンテキスト
     * @return 更新後の予算DTO
     */
    @PUT
    @Path("/{id}")
    public Response updateBudget(@PathParam("id") Long id, BudgetDto dto,
                                 @Context SecurityContext secCtx) {
        logger.info("予算更新。ID=" + id + ", ユーザー=" + secCtx.getUserPrincipal().getName());

        Budget entity = budgetMapper.toEntity(dto);
        entity.setId(id);
        Budget updated = budgetService.updateBudget(entity);
        BudgetDto resultDto = budgetMapper.toDto(updated);

        return Response.ok(resultDto).build();
    }

    // ========================================================================
    // 予算チェック・利用率
    // ========================================================================

    /**
     * 予算の利用可能額をチェックする。
     *
     * <p>【技術的負債 #12】HashMapで応答を返している。</p>
     *
     * @param budgetId 予算ID
     * @param amount   必要金額
     * @return 利用可否とメッセージ
     */
    @GET
    @Path("/check-availability")
    public Response checkAvailability(
            @QueryParam("budgetId") Long budgetId,
            @QueryParam("amount") BigDecimal amount) {

        logger.info("予算利用可否チェック。予算ID=" + budgetId + ", 金額=" + amount);

        boolean available = budgetService.checkBudgetAvailability(budgetId, amount);

        // 技術的負債 #12: HashMapで応答を返す（型安全なDTOを使うべき）
        HashMap<String, Object> result = new HashMap<>();
        result.put("budgetId", budgetId);
        result.put("requestedAmount", amount);
        result.put("available", available);
        result.put("message", available ? "予算利用可能です。" : "予算が不足しています。");

        return Response.ok(result).build();
    }

    /**
     * 予算利用率を取得する。
     *
     * <p>【技術的負債 #12】HashMapで応答を返している。</p>
     *
     * @param id 予算ID
     * @return 利用率情報
     */
    @GET
    @Path("/{id}/utilization")
    public Response getBudgetUtilization(@PathParam("id") Long id) {
        logger.info("予算利用率取得。ID=" + id);

        Budget budget = budgetService.findById(id);

        // 技術的負債 #12: HashMapで応答を返す
        HashMap<String, Object> result = new HashMap<>();
        result.put("budgetId", budget.getId());
        result.put("fiscalYear", budget.getFiscalYear());
        result.put("totalAmount", budget.getTotalAmount());
        result.put("spentAmount", budget.getSpentAmount() != null ? budget.getSpentAmount() : BigDecimal.ZERO);

        BigDecimal remaining = budget.getTotalAmount();
        if (budget.getSpentAmount() != null) {
            remaining = remaining.subtract(budget.getSpentAmount());
        }
        result.put("remainingAmount", remaining);

        if (budget.getTotalAmount() != null && budget.getTotalAmount().compareTo(BigDecimal.ZERO) > 0
                && budget.getSpentAmount() != null) {
            double rate = budget.getSpentAmount().doubleValue() / budget.getTotalAmount().doubleValue() * 100;
            result.put("utilizationPercentage", Math.round(rate * 100.0) / 100.0);
        } else {
            result.put("utilizationPercentage", 0.0);
        }

        return Response.ok(result).build();
    }

    /**
     * 会計年度をクローズする。
     *
     * @param body   クローズデータ（fiscalYear）
     * @param secCtx セキュリティコンテキスト
     * @return 処理結果
     */
    @POST
    @Path("/close-fiscal-year")
    public Response closeFiscalYear(Map<String, Object> body,
                                    @Context SecurityContext secCtx) {
        logger.info("年度クローズ。ユーザー=" + secCtx.getUserPrincipal().getName());

        int fiscalYear = Integer.parseInt(body.get("fiscalYear").toString());
        budgetService.closeFiscalYear(fiscalYear);

        Map<String, String> result = new HashMap<>();
        result.put("message", "会計年度 " + fiscalYear + " をクローズしました。");
        return Response.ok(result).build();
    }

    private void resolveDepartmentNames(List<BudgetDto> dtoList) {
        for (BudgetDto dto : dtoList) {
            if (dto.getDepartmentId() != null) {
                Department dept = em.find(Department.class, dto.getDepartmentId());
                if (dept != null) {
                    dto.setDepartmentName(dept.getName());
                }
            }
        }
    }
}
