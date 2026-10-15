# V10.3 Sync Stability Fix

- Prevented startup/scheduled IPTV-org synchronization from racing health checks.
- Fixed detached-entity re-merge in the two-phase channel/candidate persistence path by saving the managed instances returned by `saveAll()`.
- Preserved the complete V10.2 application baseline and all existing Live-TV/VOD features.
