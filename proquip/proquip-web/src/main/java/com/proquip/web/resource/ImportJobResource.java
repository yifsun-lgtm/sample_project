package com.proquip.web.resource;

import com.proquip.common.dto.admin.ImportJobDto;
import com.proquip.ejb.entity.system.ImportJob;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Path("/import-jobs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ImportJobResource {

    private static final Logger logger = Logger.getLogger(ImportJobResource.class.getName());

    @PersistenceContext
    private EntityManager em;

    @GET
    @SuppressWarnings("unchecked")
    public Response listImportJobs() {
        logger.info("インポートジョブ一覧取得。");

        List<ImportJob> jobs = em.createNamedQuery("ImportJob.findRecentJobs")
                .setMaxResults(50)
                .getResultList();

        List<ImportJobDto> dtoList = new ArrayList<>();
        for (ImportJob job : jobs) {
            dtoList.add(toDto(job));
        }

        return Response.ok(dtoList).build();
    }

    @GET
    @Path("/{id}")
    public Response getImportJob(@PathParam("id") Long id) {
        logger.info("インポートジョブ詳細取得。ID=" + id);

        ImportJob job = em.find(ImportJob.class, id);
        if (job == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"インポートジョブが見つかりません。ID: " + id + "\"}")
                    .build();
        }

        return Response.ok(toDto(job)).build();
    }

    private ImportJobDto toDto(ImportJob entity) {
        ImportJobDto dto = new ImportJobDto();
        dto.setJobId(entity.getId() != null ? entity.getId().toString() : null);
        dto.setEntityType(entity.getEntityType());
        dto.setFileName(entity.getFileName());
        dto.setStatus(entity.getStatus());
        dto.setTotalRows(entity.getTotalRecords() != null ? entity.getTotalRecords() : 0);
        dto.setProcessedRows(entity.getProcessedRecords() != null ? entity.getProcessedRecords() : 0);
        dto.setErrorCount(entity.getErrorRecords() != null ? entity.getErrorRecords() : 0);
        dto.setSuccessCount(dto.getProcessedRows() - dto.getErrorCount());
        dto.setProgress(entity.getProgressPercentage());
        dto.setStartedAt(entity.getStartedAt());
        dto.setCompletedAt(entity.getCompletedAt());
        dto.setStartedBy(entity.getStartedBy());
        return dto;
    }
}
