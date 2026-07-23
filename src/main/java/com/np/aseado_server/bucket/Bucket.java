package com.np.aseado_server.bucket;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * A published profile's identity on the server — the permanent part.
 * Mirrors the desktop's Profile concept 1:1 (see ASEADO desktop's
 * registry.Profile): {@code name} is the free-text label an admin picks
 * (e.g. "1st Sem"), {@code mode} is "V1" or "V2", and {@code departmentLabel}
 * is only meaningful for V1 (a V1 profile is locked to one department;
 * V2 profiles span many departments per-student in their roster, so
 * there's no single department to show for those — left null).
 *
 * Event-specific data (name, date, cutoff, filters) deliberately does
 * NOT live here — that belongs to whichever {@link com.np.aseado_server.session.ReceivingSession}
 * is currently active, since one bucket goes through many separate
 * receiving cycles over its lifetime, each with its own event.
 */
@Entity
@Table(name = "buckets")
public class Bucket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String mode; // "V1" | "V2" — kept as a plain string, not an enum,
                          // so the server never has to be redeployed just
                          // because the desktop adds a new mode someday.

    private String departmentLabel; // null for V2

    /**
     * Raw student-list CSV for this profile — the source of truth Android
     * downloads once a bucket is open, so it can tell "known" from
     * "unknown" entirely offline while scanning. Stored as a raw blob,
     * same pattern the desktop app already uses for filterJson, rather
     * than a normalized student table — this server never reads into it
     * itself, it's just custody until Android or desktop needs it.
     * Lives on the bucket (not the session) since a roster is a property
     * of the profile and doesn't necessarily change every receiving
     * cycle — but downloading it is still gated behind an active session
     * key (see BucketService#downloadRoster), so "no open bucket" still
     * means "no roster available" from Android's point of view.
     */
    @Column(columnDefinition = "TEXT")
    private String rosterCsv;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BucketStatus status = BucketStatus.OFF;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Bucket() {} // JPA

    public Bucket(String name, String mode, String departmentLabel) {
        this.name = name;
        this.mode = mode;
        this.departmentLabel = departmentLabel;
    }

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getDepartmentLabel() { return departmentLabel; }
    public void setDepartmentLabel(String departmentLabel) { this.departmentLabel = departmentLabel; }

    public BucketStatus getStatus() { return status; }
    public void setStatus(BucketStatus status) { this.status = status; }

    public String getRosterCsv() { return rosterCsv; }
    public void setRosterCsv(String rosterCsv) { this.rosterCsv = rosterCsv; }

    public Instant getCreatedAt() { return createdAt; }
}
