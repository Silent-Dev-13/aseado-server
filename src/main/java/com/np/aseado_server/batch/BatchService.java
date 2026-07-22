package com.np.aseado_server.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.np.aseado_server.bucket.Bucket;
import com.np.aseado_server.bucket.BucketService;
import com.np.aseado_server.exception.ApiException;
import com.np.aseado_server.session.ReceivingSession;
import com.np.aseado_server.session.ReceivingSessionRepository;
import com.np.aseado_server.web.dto.BatchDtos.ScanRecordDto;
import com.np.aseado_server.web.dto.BatchDtos.UploadBatchRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BatchService {

    private final BatchUploadRepository batches;
    private final BucketService bucketService;
    private final ReceivingSessionRepository sessions;
    private final ObjectMapper mapper = new ObjectMapper();

    public BatchService(BatchUploadRepository batches, BucketService bucketService,
                         ReceivingSessionRepository sessions) {
        this.batches = batches;
        this.bucketService = bucketService;
        this.sessions = sessions;
    }

    /** Android's upload — key must match the bucket's currently open session. */
    public BatchUpload upload(Long bucketId, UploadBatchRequest req) {
        Bucket bucket = bucketService.get(bucketId);
        ReceivingSession session = bucketService.activeSession(bucket.getId())
                .orElseThrow(() -> ApiException.conflict("This bucket isn't receiving right now"));

        if (!session.getAccessKey().equals(req.key())) {
            throw ApiException.unauthorized("Invalid or expired key for this bucket");
        }

        String json = writeRecords(req.records());
        BatchUpload upload = new BatchUpload(session.getId(), json, req.records().size());
        return batches.save(upload);
    }

    /** Desktop pulls everything still pending across every session this bucket has ever had. */
    public List<BatchUpload> pendingFor(Long bucketId) {
        List<Long> sessionIds = sessions.findByBucketId(bucketId).stream().map(ReceivingSession::getId).toList();
        if (sessionIds.isEmpty()) return List.of();
        return batches.findBySessionIdInAndStatus(sessionIds, BatchUploadStatus.PENDING);
    }

    public List<ScanRecordDto> readRecords(BatchUpload upload) {
        try {
            CollectionType listType = mapper.getTypeFactory().constructCollectionType(List.class, ScanRecordDto.class);
            return mapper.readValue(upload.getRecordsJson(), listType);
        } catch (Exception e) {
            throw new IllegalStateException("Corrupt batch records for batch " + upload.getId(), e);
        }
    }

    private String writeRecords(List<ScanRecordDto> records) {
        try {
            return mapper.writeValueAsString(records);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize batch records", e);
        }
    }

    public BatchUpload get(Long id) {
        return batches.findById(id).orElseThrow(() -> ApiException.notFound("Batch not found"));
    }

    public BatchUpload accept(Long id) {
        BatchUpload b = get(id);
        requirePending(b);
        b.accept();
        return batches.save(b);
    }

    public BatchUpload refuse(Long id) {
        BatchUpload b = get(id);
        requirePending(b);
        b.refuse();
        return batches.save(b);
    }

    private void requirePending(BatchUpload b) {
        if (b.getStatus() != BatchUploadStatus.PENDING) {
            throw ApiException.conflict("Batch " + b.getId() + " was already " + b.getStatus());
        }
    }
}
