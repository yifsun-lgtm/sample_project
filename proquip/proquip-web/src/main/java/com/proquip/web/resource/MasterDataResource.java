package com.proquip.web.resource;

import com.proquip.ejb.entity.product.Manufacturer;
import com.proquip.ejb.entity.product.UnitOfMeasure;
import com.proquip.ejb.entity.pricing.Currency;
import com.proquip.ejb.entity.pricing.TaxRate;

import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Path("/master-data")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMIN", "MANAGER"})
public class MasterDataResource {

    private static final Logger logger = Logger.getLogger(MasterDataResource.class.getName());

    @PersistenceContext
    private EntityManager em;

    // ========== Manufacturers ==========

    @GET
    @Path("/manufacturers")
    public Response listManufacturers() {
        List<Manufacturer> list = em.createQuery(
                "SELECT m FROM Manufacturer m ORDER BY m.name", Manufacturer.class)
                .getResultList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Manufacturer m : list) {
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

    @POST
    @Path("/manufacturers")
    public Response createManufacturer(Manufacturer entity) {
        em.persist(entity);
        return Response.status(Response.Status.CREATED).entity(entity).build();
    }

    @PUT
    @Path("/manufacturers/{id}")
    public Response updateManufacturer(@PathParam("id") Long id, Manufacturer entity) {
        Manufacturer existing = em.find(Manufacturer.class, id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        existing.setName(entity.getName());
        existing.setCode(entity.getCode());
        existing.setWebsite(entity.getWebsite());
        existing.setCountry(entity.getCountry());
        em.merge(existing);
        return Response.ok(existing).build();
    }

    @DELETE
    @Path("/manufacturers/{id}")
    public Response deleteManufacturer(@PathParam("id") Long id) {
        Manufacturer existing = em.find(Manufacturer.class, id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        em.remove(existing);
        return Response.noContent().build();
    }

    // ========== Units of Measure ==========

    @GET
    @Path("/units")
    public Response listUnits() {
        List<UnitOfMeasure> list = em.createQuery(
                "SELECT u FROM UnitOfMeasure u ORDER BY u.name", UnitOfMeasure.class)
                .getResultList();
        return Response.ok(list).build();
    }

    @POST
    @Path("/units")
    public Response createUnit(UnitOfMeasure entity) {
        em.persist(entity);
        return Response.status(Response.Status.CREATED).entity(entity).build();
    }

    @PUT
    @Path("/units/{id}")
    public Response updateUnit(@PathParam("id") Long id, UnitOfMeasure entity) {
        UnitOfMeasure existing = em.find(UnitOfMeasure.class, id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        existing.setName(entity.getName());
        existing.setCode(entity.getCode());
        em.merge(existing);
        return Response.ok(existing).build();
    }

    @DELETE
    @Path("/units/{id}")
    public Response deleteUnit(@PathParam("id") Long id) {
        UnitOfMeasure existing = em.find(UnitOfMeasure.class, id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        em.remove(existing);
        return Response.noContent().build();
    }

    // ========== Currencies ==========

    @GET
    @Path("/currencies")
    public Response listCurrencies() {
        List<Currency> list = em.createQuery(
                "SELECT c FROM Currency c ORDER BY c.code", Currency.class)
                .getResultList();
        return Response.ok(list).build();
    }

    @POST
    @Path("/currencies")
    public Response createCurrency(Currency entity) {
        em.persist(entity);
        return Response.status(Response.Status.CREATED).entity(entity).build();
    }

    @PUT
    @Path("/currencies/{id}")
    public Response updateCurrency(@PathParam("id") Long id, Currency entity) {
        Currency existing = em.find(Currency.class, id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        existing.setName(entity.getName());
        existing.setCode(entity.getCode());
        existing.setSymbol(entity.getSymbol());
        existing.setExchangeRate(entity.getExchangeRate());
        em.merge(existing);
        return Response.ok(existing).build();
    }

    // ========== Tax Rates ==========

    @GET
    @Path("/tax-rates")
    public Response listTaxRates() {
        List<TaxRate> list = em.createQuery(
                "SELECT t FROM TaxRate t ORDER BY t.code", TaxRate.class)
                .getResultList();
        return Response.ok(list).build();
    }

    @POST
    @Path("/tax-rates")
    public Response createTaxRate(TaxRate entity) {
        em.persist(entity);
        return Response.status(Response.Status.CREATED).entity(entity).build();
    }

    @PUT
    @Path("/tax-rates/{id}")
    public Response updateTaxRate(@PathParam("id") Long id, TaxRate entity) {
        TaxRate existing = em.find(TaxRate.class, id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        existing.setName(entity.getName());
        existing.setCode(entity.getCode());
        existing.setRate(entity.getRate());
        existing.setCountry(entity.getCountry());
        existing.setEffectiveFrom(entity.getEffectiveFrom());
        existing.setEffectiveTo(entity.getEffectiveTo());
        em.merge(existing);
        return Response.ok(existing).build();
    }
}
