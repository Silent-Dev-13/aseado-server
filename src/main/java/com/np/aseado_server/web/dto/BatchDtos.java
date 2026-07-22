package com.np.aseado_server.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class BatchDtos {

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
            @NotEmpty List<ScanRecordDto> records
    ) {}

    public record UploadBatchResponse(Long batchId, int recordCount) {}

    /** Desktop -> server: what's waiting to be reviewed for a bucket. */
    public record PendingBatchResponse(
            Long batchId, Long sessionId, String uploadedAt,
            List<ScanRecordDto> records
    ) {}

    public record BatchActionResponse(Long batchId, String status) {}
}
