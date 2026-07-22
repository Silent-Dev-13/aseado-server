package com.np.aseado_server.web.dto;

import jakarta.validation.constraints.NotBlank;

public class BucketDtos {

    /** Desktop -> server: publish a profile as a bucket. Just identity, no event data. */
    public record CreateBucketRequest(
            @NotBlank String name,
            @NotBlank String mode,          // "V1" | "V2"
            String departmentLabel          // required for V1, ignored/null for V2
    ) {}

    public record UpdateBucketRequest(
            String name,
            String departmentLabel
    ) {}

    /** Desktop's own view of one of its buckets. No event data here —
     *  a bucket never carries any; see BatchDtos.EventMetaDto for where
     *  event metadata actually lives (attached to each upload). */
    public record BucketResponse(
            Long id, String name, String mode, String departmentLabel, String status
    ) {}

    /** What Android sees in discovery — only RECEIVING buckets, never a key. */
    public record DiscoverBucketResponse(
            Long id, String name, String mode, String departmentLabel
    ) {}

    /** Desktop -> server: bare toggle, OFF -> RECEIVING. No body needed —
     *  opening a bucket doesn't decide anything about an event, Android
     *  does that entirely on its own once it's scanning. */
    public record OpenReceivingResponse(Long sessionId, String key) {}

    public record VerifyKeyRequest(@NotBlank String key) {}

    public record VerifyKeyResponse(boolean valid, Long sessionId, boolean rosterAvailable) {}

    /** Desktop -> server: publish/replace the student-list CSV for a bucket. */
    public record UploadRosterRequest(@NotBlank String csv) {}

    /** Android -> server: pull the roster, key-gated same as batch upload. */
    public record DownloadRosterRequest(@NotBlank String key) {}

    public record RosterResponse(String csv, int studentCount) {}
}
