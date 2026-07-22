package com.np.aseado_server.bucket;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BucketRepository extends JpaRepository<Bucket, Long> {
    List<Bucket> findByStatus(BucketStatus status);
}
