package com.np.aseado_server.bucket;

/**
 * A bucket only ever has two states. There's deliberately no third
 * "CLOSED"/archived state — per the design discussion, a bucket is meant
 * to be opened for receiving repeatedly over its lifetime (e.g. every
 * semester), so "not currently receiving" is just OFF, same as it was
 * before it was ever opened the first time. Deleting a bucket is a
 * separate, explicit action (BucketController#delete), not a status.
 */
public enum BucketStatus {
    OFF,
    RECEIVING
}
