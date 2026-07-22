package com.np.aseado_server.web;

import com.np.aseado_server.batch.BatchUpload;
import com.np.aseado_server.batch.BatchService;
import com.np.aseado_server.web.dto.BatchDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BatchController {

    private final BatchService service;

    public BatchController(BatchService service) {
        this.service = service;
    }

    // ── Android-facing: public endpoint, but the key inside the body is
    //    what actually authorizes the write — see AdminKeyFilter#isPublic
    //    and BatchService#upload. ──
    @PostMapping("/api/buckets/{id}/batches")
    @ResponseStatus(HttpStatus.CREATED)
    public UploadBatchResponse upload(@PathVariable Long id, @Valid @RequestBody UploadBatchRequest req) {
        BatchUpload upload = service.upload(id, req);
        return new UploadBatchResponse(upload.getId(), upload.getRecordCount());
    }

    // ── Desktop admin (X-Admin-Key required) ──

    @GetMapping("/api/buckets/{id}/batches/pending")
    public List<PendingBatchResponse> pending(@PathVariable Long id) {
        return service.pendingFor(id).stream()
                .map(b -> new PendingBatchResponse(
                        b.getId(), b.getSessionId(), b.getUploadedAt().toString(), service.readRecords(b)))
                .toList();
    }

    @PostMapping("/api/batches/{id}/accept")
    public BatchActionResponse accept(@PathVariable Long id) {
        BatchUpload b = service.accept(id);
        return new BatchActionResponse(b.getId(), b.getStatus().name());
    }

    @PostMapping("/api/batches/{id}/refuse")
    public BatchActionResponse refuse(@PathVariable Long id) {
        BatchUpload b = service.refuse(id);
        return new BatchActionResponse(b.getId(), b.getStatus().name());
    }
}
