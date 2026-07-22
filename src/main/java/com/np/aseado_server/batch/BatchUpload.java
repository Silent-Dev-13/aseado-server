package com.np.aseado_server.batch;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * One upload Android made into an open receiving session. Carries its
 * own event metadata now (name, date, login cutoff, filters, logout
 * enabled) — Android decided all of this itself, entirely offline, and
 * sends it along with the scan records rather than desktop deciding it
 * ahead of time. Desktop creates the actual local event out of this data
 * the moment it accepts the batch (see aseado-backend's
 * CloudSyncController#accept), not before.
 *
 * Records are stored as a raw JSON blob (recordsJson) rather than a
 * normalized child table — this server never reads into individual
 * record fields itself (it's a relay/mailbox, not the domain-logic
 * engine), it just hands the whole thing back to desktop on GET
 * /pending for BruteForceService to actually process.
 *
 * Belongs to a session, not directly to a bucket — so if a bucket is
 * closed and reopened later, old batches stay attached to the session
 * they arrived under and don't get mixed in with a fresh cycle's
 * uploads.
 */
@Entity
@Table(name = "batch_uploads")
public class BatchUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sessionId;

    // ── Event metadata, as Android decided it ──
    @Column(nullable = false)
    private String eventName;

    private String eventDate;
    private String loginTimeLimit;
    private boolean hasLogout;

    @Lob
    private String filterJson;

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

    public BatchUpload(Long sessionId, String eventName, String eventDate, String loginTimeLimit,
                        boolean hasLogout, String filterJson, String recordsJson, int recordCount) {
        this.sessionId = sessionId;
        this.eventName = eventName;
        this.eventDate = eventDate;
        this.loginTimeLimit = loginTimeLimit;
        this.hasLogout = hasLogout;
        this.filterJson = filterJson;
        this.recordsJson = recordsJson;
        this.recordCount = recordCount;
    }

    public void accept() { this.status = BatchUploadStatus.ACCEPTED; this.reviewedAt = Instant.now(); }
    public void refuse() { this.status = BatchUploadStatus.REFUSED; this.reviewedAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getSessionId() { return sessionId; }
    public String getEventName() { return eventName; }
    public String getEventDate() { return eventDate; }
    public String getLoginTimeLimit() { return loginTimeLimit; }
    public boolean isHasLogout() { return hasLogout; }
    public String getFilterJson() { return filterJson; }
    public String getRecordsJson() { return recordsJson; }
    public int getRecordCount() { return recordCount; }
    public BatchUploadStatus getStatus() { return status; }
    public Instant getUploadedAt() { return uploadedAt; }
    public Instant getReviewedAt() { return reviewedAt; }
}
