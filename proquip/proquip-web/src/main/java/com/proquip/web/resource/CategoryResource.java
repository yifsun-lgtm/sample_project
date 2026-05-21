package com.proquip.web.resource;

import com.proquip.common.dto.product.CategoryDto;
import com.proquip.ejb.entity.product.Category;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Path("/categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CategoryResource {

    private static final Logger logger = Logger.getLogger(CategoryResource.class.getName());

    @PersistenceContext
    private EntityManager em;

    @GET
    @SuppressWarnings("unchecked")
    public Response listCategories() {
        logger.info("カテゴリ一覧取得。");

        List<Category> categories = em.createNamedQuery("Category.findRootCategories")
                .getResultList();

        List<CategoryDto> dtoList = new ArrayList<>();
        for (Category cat : categories) {
            dtoList.add(toDto(cat));
        }

        return Response.ok(dtoList).build();
    }

    private CategoryDto toDto(Category entity) {
        CategoryDto dto = new CategoryDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setLevel(entity.getLevel() != null ? entity.getLevel() : 0);
        dto.setActive(true);

        if (entity.getParent() != null) {
            dto.setParentId(entity.getParent().getId());
            dto.setParentName(entity.getParent().getName());
        }

        try {
            if (entity.getProducts() != null) {
                dto.setProductCount(entity.getProducts().size());
            }
        } catch (Exception e) {
            dto.setProductCount(0);
        }

        try {
            if (entity.getChildren() != null && !entity.getChildren().isEmpty()) {
                List<CategoryDto> children = new ArrayList<>();
                for (Category child : entity.getChildren()) {
                    children.add(toDto(child));
                }
                dto.setChildren(children);
            }
        } catch (Exception e) {
            // lazy load failure
        }

        return dto;
    }
}
