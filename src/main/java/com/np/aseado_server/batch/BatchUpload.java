package com.np.aseado_server.batch;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * One upload Android made into an open receiving session. Records are
 * stored as a raw JSON blob (recordsJson) rather than a normalized child
 * table — this server never reads into individual record fields itself
 * (it's a relay/mailbox, not the domain-logic engine), it just hands the
 * whole blob back to desktop on GET /pending for BruteForceService to
 * actually process. Same "store the JSON, don't model it" pattern the
 * desktop app already uses for event filterJson.
 *
 * Deliberately belongs to a session, not directly to a bucket — so if a
 * bucket is closed and reopened later, old batches stay attached to the
 * session they arrived under and don't get mixed in with a fresh cycle's
 * uploads (this was an open question earlier in the design; this is the
 * resolution — session-scoped, not bucket-scoped).
 */
@Entity
@Table(name = "batch_uploads")
public class BatchUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sessionId;

    @Lob
    @Column(nullable = false)
    private String recordsJson;

    @Column(nullable = false)
    private int recordCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchUploadStatus status = BatchUploadStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private Instant uploadedAt = Instant.now();

    private Instant reviewedAt; // null until accepted/refused

    protected BatchUpload() {} // JPA

    public BatchUpload(Long sessionId, String recordsJson, int recordCount) {
        this.sessionId = sessionId;
        this.recordsJson = recordsJson;
        this.recordCount = recordCount;
    }

    public void accept() { this.status = BatchUploadStatus.ACCEPTED; this.reviewedAt = Instant.now(); }
    public void refuse() { this.status = BatchUploadStatus.REFUSED; this.reviewedAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getSessionId() { return sessionId; }
    public String getRecordsJson() { return recordsJson; }
    public int getRecordCount() { return recordCount; }
    public BatchUploadStatus getStatus() { return status; }
    public Instant getUploadedAt() { return uploadedAt; }
    public Instant getReviewedAt() { return reviewedAt; }
}
