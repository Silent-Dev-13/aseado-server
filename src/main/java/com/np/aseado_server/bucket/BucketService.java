package com.np.aseado_server.bucket;

import com.np.aseado_server.exception.ApiException;
import com.np.aseado_server.session.ReceivingSession;
import com.np.aseado_server.session.ReceivingSessionRepository;
import com.np.aseado_server.web.dto.BucketDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

@Service
public class BucketService {

    // Excludes visually-confusing characters (0/O, 1/I/l) since a human
    // may need to type this key in on a phone keyboard.
    private static final String KEY_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int KEY_LENGTH = 8;
    private final SecureRandom random = new SecureRandom();

    private final BucketRepository buckets;
    private final ReceivingSessionRepository sessions;

    public BucketService(BucketRepository buckets, ReceivingSessionRepository sessions) {
        this.buckets = buckets;
        this.sessions = sessions;
    }

    public Bucket create(CreateBucketRequest req) {
        if ("V1".equals(req.mode()) && (req.departmentLabel() == null || req.departmentLabel().isBlank())) {
            throw ApiException.badRequest("V1 buckets need a departmentLabel");
        }
        String dept = "V2".equals(req.mode()) ? null : req.departmentLabel();
        return buckets.save(new Bucket(req.name(), req.mode(), dept));
    }

    public Bucket get(Long id) {
        return buckets.findById(id).orElseThrow(() -> ApiException.notFound("Bucket not found"));
    }

    public List<Bucket> listAll() {
        return buckets.findAll();
    }

    public List<Bucket> listReceiving() {
        return buckets.findByStatus(BucketStatus.RECEIVING);
    }

    public Bucket update(Long id, UpdateBucketRequest req) {
        Bucket b = get(id);
        if (req.name() != null && !req.name().isBlank()) b.setName(req.name());
        if ("V1".equals(b.getMode()) && req.departmentLabel() != null) b.setDepartmentLabel(req.departmentLabel());
        return buckets.save(b);
    }

    public void delete(Long id) {
        Bucket b = get(id);
        sessions.findFirstByBucketIdAndClosedAtIsNull(b.getId()).ifPresent(ReceivingSession::close);
        buckets.delete(b);
    }

    /** Flip OFF -> RECEIVING: opens a fresh session with its own event metadata and key. */
    @Transactional
    public ReceivingSession openReceiving(Long id, OpenReceivingRequest req) {
        Bucket b = get(id);
        if (b.getStatus() == BucketStatus.RECEIVING) {
            throw ApiException.conflict("Bucket is already receiving — close it before opening a new session");
        }
        String key = generateKey();
        ReceivingSession session = new ReceivingSession(
                b.getId(), key, req.eventName(), req.eventDate(),
                req.loginTimeLimit(), req.hasLogout(), req.filterJson());
        sessions.save(session);
        b.setStatus(BucketStatus.RECEIVING);
        buckets.save(b);
        return session;
    }

    /** Flip RECEIVING -> OFF: closes the active session, invalidating its key. */
    @Transactional
    public void closeReceiving(Long id) {
        Bucket b = get(id);
        sessions.findFirstByBucketIdAndClosedAtIsNull(b.getId())
                .ifPresent(ReceivingSession::close);
        b.setStatus(BucketStatus.OFF);
        buckets.save(b);
    }

    public Optional<ReceivingSession> activeSession(Long bucketId) {
        return sessions.findFirstByBucketIdAndClosedAtIsNull(bucketId);
    }

    /** Only a currently-open session's key is valid — a closed session's key is dead. */
    public Optional<ReceivingSession> verifyKey(String key) {
        return sessions.findByAccessKey(key).filter(ReceivingSession::isOpen);
    }

    /** Desktop publishes/replaces the roster CSV for a bucket. No parsing/validation
     *  here on purpose — this server treats it as an opaque blob, same as filterJson;
     *  desktop and Android are the ones that actually understand CSV columns. */
    public Bucket uploadRoster(Long id, String csv) {
        Bucket b = get(id);
        b.setRosterCsv(csv);
        return buckets.save(b);
    }

    /** Android pulls the roster — only works with a valid key for a session that's
     *  actually open on THIS bucket, same gating as batch upload. */
    public String downloadRoster(Long id, String key) {
        Bucket b = get(id);
        verifyKey(key)
                .filter(s -> s.getBucketId().equals(id))
                .orElseThrow(() -> ApiException.unauthorized("Invalid or expired key for this bucket"));

        if (b.getRosterCsv() == null || b.getRosterCsv().isBlank()) {
            throw ApiException.notFound("No roster has been uploaded for this bucket yet");
        }
        return b.getRosterCsv();
    }

    public static int countRows(String csv) {
        if (csv == null || csv.isBlank()) return 0;
        long lines = csv.lines().filter(l -> !l.isBlank()).count();
        return (int) Math.max(0, lines - 1); // minus header row
    }

    private String generateKey() {
        StringBuilder sb = new StringBuilder(KEY_LENGTH);
        for (int i = 0; i < KEY_LENGTH; i++) {
            sb.append(KEY_ALPHABET.charAt(random.nextInt(KEY_ALPHABET.length())));
        }
        return sb.toString();
    }
}
