package com.np.aseado_server.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class BatchDtos {

    /**
     * The event Android decided on its own, entirely offline — name,
     * date, login cutoff, filters, whether logout is enabled. Bundled
     * into the batch upload rather than desktop pre-deciding it, since
     * there's often no desktop around to ask when this gets created.
     * Mirrors desktop's own CreateEventRequest shape 1:1 so accepting a
     * batch can create the local event with zero translation.
     */
    public record EventMetaDto(
            @NotBlank String eventName, String eventDate, String loginTimeLimit,
            boolean hasLogout, String filterJson
    ) {}

    /**
     * One scanned student, as Android assembled it locally. loginTime is
     * required (every record represents at least a login); logoutTime is
     * optional (present only if the same ID was scanned again on the
     * phone and the device merged it into this same record — no
     * login/logout toggle guessing happens on the server or desktop,
     * Android already resolved that before upload).
     *
     * proofName/proofYear/proofProgram are never trusted as fact — they
     * only exist to pre-fill desktop's manual-entry form for unknown IDs.
     * A known student ID always uses the real roster data instead.
     */
    public record ScanRecordDto(
            @NotBlank String studentId,
            String proofName,
            String proofYear,      // operator-typed on the phone, optional
            String proofProgram,   // operator-typed on the phone, optional — V1 buckets won't have this asked
            @NotBlank String loginTime,   // ISO datetime, the phone's own clock
            String logoutTime             // ISO datetime, optional
    ) {}

    /** Android -> server: upload a completed batch into the currently open session. */
    public record UploadBatchRequest(
            @NotBlank String key,
            EventMetaDto eventMeta,
            @NotEmpty List<ScanRecordDto> records
    ) {}

    public record UploadBatchResponse(Long batchId, int recordCount) {}

    /** Desktop -> server: what's waiting to be reviewed for a bucket. */
    public record PendingBatchResponse(
            Long batchId, Long sessionId, String uploadedAt,
            EventMetaDto eventMeta, List<ScanRecordDto> records
    ) {}

    public record BatchActionResponse(Long batchId, String status) {}
}
