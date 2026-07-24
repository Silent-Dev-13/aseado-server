package com.np.aseado_server.web;

import com.np.aseado_server.bucket.Bucket;
import com.np.aseado_server.bucket.BucketService;
import com.np.aseado_server.session.ReceivingSession;
import com.np.aseado_server.web.dto.BucketDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/buckets")
public class BucketController {

    private final BucketService service;

    public BucketController(BucketService service) {
        this.service = service;
    }

    // ── Desktop admin (X-Admin-Key required — enforced by AdminKeyFilter) ──

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BucketResponse create(@Valid @RequestBody CreateBucketRequest req) {
        return toResponse(service.create(req));
    }

    @GetMapping
    public List<BucketResponse> listAll() {
        return service.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public BucketResponse get(@PathVariable Long id) {
        return toResponse(service.get(id));
    }

    @PatchMapping("/{id}")
    public BucketResponse update(@PathVariable Long id, @RequestBody UpdateBucketRequest req) {
        return toResponse(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    /** Bare toggle — no body. Opening a bucket doesn't decide anything
     *  about an event; Android does that entirely on its own once it's
     *  actually scanning, and sends it along with the batch upload. */
    @PostMapping("/{id}/open")
    public OpenReceivingResponse open(@PathVariable Long id) {
        ReceivingSession session = service.openReceiving(id);
        return new OpenReceivingResponse(session.getId(), session.getAccessKey());
    }

    @PostMapping("/{id}/close")
    public BucketResponse close(@PathVariable Long id) {
        service.closeReceiving(id);
        return toResponse(service.get(id));
    }

    // ── Android-facing (public / key-scoped — see AdminKeyFilter#isPublic) ──

    @GetMapping("/discover")
    public List<DiscoverBucketResponse> discover() {
        return service.listReceiving().stream()
                .map(b -> new DiscoverBucketResponse(b.getId(), b.getName(), b.getMode(), b.getDepartmentLabel()))
                .toList();
    }

    @PostMapping("/{id}/verify-key")
    public VerifyKeyResponse verifyKey(@PathVariable Long id, @Valid @RequestBody VerifyKeyRequest req) {
        Optional<ReceivingSession> match = service.verifyKey(req.key())
                .filter(s -> s.getBucketId().equals(id));
        if (match.isEmpty()) return new VerifyKeyResponse(false, null, false);
        Bucket b = service.get(id);
        boolean rosterAvailable = b.getRosterCsv() != null && !b.getRosterCsv().isBlank();
        return new VerifyKeyResponse(true, match.get().getId(), rosterAvailable);
    }

    // ── Roster: desktop publishes it (admin key), Android pulls it (bucket key) ──

    @PostMapping("/{id}/roster")
    public RosterResponse uploadRoster(@PathVariable Long id, @Valid @RequestBody UploadRosterRequest req) {
        Bucket b = service.uploadRoster(id, req.csv());
        return new RosterResponse(null, BucketService.countRows(b.getRosterCsv()));
    }

    @PostMapping("/{id}/roster/download")
    public RosterResponse downloadRoster(@PathVariable Long id, @Valid @RequestBody DownloadRosterRequest req) {
        String csv = service.downloadRoster(id, req.key());
        return new RosterResponse(csv, BucketService.countRows(csv));
    }

    // ── mapping helpers ──

    private BucketResponse toResponse(Bucket b) {
        String activeKey = service.activeSession(b.getId())
                .map(ReceivingSession::getAccessKey)
                .orElse(null);
        boolean rosterUploaded = b.getRosterCsv() != null && !b.getRosterCsv().isBlank();
        int rosterCount = BucketService.countRows(b.getRosterCsv());
        
        return new BucketResponse(
                b.getId(), 
                b.getName(), 
                b.getMode(), 
                b.getDepartmentLabel(), 
                b.getStatus().name(),
                activeKey,
                rosterUploaded,
                rosterCount
        );
    }
}
