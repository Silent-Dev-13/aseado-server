package com.np.aseado_server.session;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * One "receiving cycle" for a bucket — created the moment a desktop admin
 * flips a bucket OFF -> RECEIVING, and it owns everything specific to
 * that cycle: the event metadata (mirrors the desktop's own
 * CreateEventRequest shape: name, date, login cutoff, filters, whether
 * logout is enabled) and a freshly-generated key.
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

    // ── Event metadata for this cycle (mirrors desktop's CreateEventRequest) ──
    @Column(nullable = false)
    private String eventName;

    private String eventDate;          // ISO date string, desktop's own format
    private String loginTimeLimit;     // ISO datetime string, nullable = no cutoff
    private boolean hasLogout;

    @Lob
    private String filterJson;         // same shape as desktop's event filterJson

    @Column(nullable = false, updatable = false)
    private Instant openedAt = Instant.now();

    private Instant closedAt; // null while still open

    protected ReceivingSession() {} // JPA

    public ReceivingSession(Long bucketId, String accessKey, String eventName, String eventDate,
                             String loginTimeLimit, boolean hasLogout, String filterJson) {
        this.bucketId = bucketId;
        this.accessKey = accessKey;
        this.eventName = eventName;
        this.eventDate = eventDate;
        this.loginTimeLimit = loginTimeLimit;
        this.hasLogout = hasLogout;
        this.filterJson = filterJson;
    }

    public boolean isOpen() { return closedAt == null; }
    public void close() { this.closedAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getBucketId() { return bucketId; }
    public String getAccessKey() { return accessKey; }
    public String getEventName() { return eventName; }
    public String getEventDate() { return eventDate; }
    public String getLoginTimeLimit() { return loginTimeLimit; }
    public boolean isHasLogout() { return hasLogout; }
    public String getFilterJson() { return filterJson; }
    public Instant getOpenedAt() { return openedAt; }
    public Instant getClosedAt() { return closedAt; }
}
