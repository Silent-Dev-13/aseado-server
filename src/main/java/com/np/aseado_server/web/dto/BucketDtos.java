package com.np.aseado_server.web.dto;

import jakarta.validation.constraints.NotBlank;

public class BucketDtos {

    /** Desktop -> server: publish a profile as a bucket. Just identity, no event data yet. */
    public record CreateBucketRequest(
            @NotBlank String name,
            @NotBlank String mode,          // "V1" | "V2"
            String departmentLabel          // required for V1, ignored/null for V2
    ) {}

    public record UpdateBucketRequest(
            String name,
            String departmentLabel
    ) {}

    /** Event metadata for whichever receiving session is currently active, if any. */
    public record EventMetaResponse(
            String eventName, String eventDate, String loginTimeLimit,
            boolean hasLogout, String filterJson
    ) {}

    /** Desktop's own view of one of its buckets (includes status, no key). */
    public record BucketResponse(
            Long id, String name, String mode, String departmentLabel, String status,
            EventMetaResponse activeEventMeta
    ) {}

    /** What Android sees in discovery — only RECEIVING buckets, never a key. */
    public record DiscoverBucketResponse(
            Long id, String name, String mode, String departmentLabel,
            EventMetaResponse eventMeta
    ) {}

    /** Desktop -> server: open a bucket for receiving, i.e. start a new session. */
    public record OpenReceivingRequest(
            @NotBlank String eventName, String eventDate, String loginTimeLimit,
            boolean hasLogout, String filterJson
    ) {}

    public record OpenReceivingResponse(Long sessionId, String key) {}

    public record VerifyKeyRequest(@NotBlank String key) {}

    public record VerifyKeyResponse(boolean valid, Long sessionId, EventMetaResponse eventMeta, boolean rosterAvailable) {}

    /** Desktop -> server: publish/replace the student-list CSV for a bucket. */
    public record UploadRosterRequest(@NotBlank String csv) {}

    /** Android -> server: pull the roster, key-gated same as batch upload. */
    public record DownloadRosterRequest(@NotBlank String key) {}

    public record RosterResponse(String csv, int studentCount) {}
}
