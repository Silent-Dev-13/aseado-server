package com.np.aseado_server.session;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * One "receiving cycle" for a bucket — created the moment a desktop admin
 * flips a bucket OFF -> RECEIVING. Deliberately carries NO event metadata
 * — that was an earlier design mistake (desktop pre-deciding the event
 * before any scanning happened). The real flow: Android decides the
 * event entirely on its own, offline, and sends its metadata *with the
 * batch upload* (see BatchUpload) — this session is just a bare
 * key-with-a-lifetime that gates discovery/roster-download/upload while
 * it's open.
 *
 * The key is session-lived, not single-use: it stays valid for every
 * upload Android makes for as long as this session is open, and stops
 * working the instant the session closes (bucket flips back to OFF) —
 * see the design discussion on why single-use was rejected (an offline
 * device may need to upload more than once before a desktop is back
 * online to review it).
 */
@Entity
@Table(name = "receiving_sessions")
public class ReceivingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long bucketId;

    @Column(nullable = false, unique = true)
    private String accessKey;

    @Column(nullable = false, updatable = false)
    private Instant openedAt = Instant.now();

    private Instant closedAt; // null while still open

    protected ReceivingSession() {} // JPA

    public ReceivingSession(Long bucketId, String accessKey) {
        this.bucketId = bucketId;
        this.accessKey = accessKey;
    }

    public boolean isOpen() { return closedAt == null; }
    public void close() { this.closedAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getBucketId() { return bucketId; }
    public String getAccessKey() { return accessKey; }
    public Instant getOpenedAt() { return openedAt; }
    public Instant getClosedAt() { return closedAt; }
}
