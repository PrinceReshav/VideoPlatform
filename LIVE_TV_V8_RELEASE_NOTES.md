# Live TV V8

## Playback compatibility
- Dedicated IPTV compatibility HTTP client for Live TV upstreams only.
- Accepts HTTP and HTTPS IPTV sources, including upstreams whose TLS certificate chain is not trusted by the JVM default trust store.
- Hostname/certificate compatibility is isolated to the IPTV outbound client; the rest of the JVM/Spring HTTPS stack is unchanged.
- Preserves IPTV-org Referer and User-Agent metadata per stream candidate.
- Keeps all local M3U candidates and tries them in order.
- Retries transient 408/425/429/5xx responses and connection/time-out failures before moving to another candidate.
- HLS manifests, variants, segments, keys and maps continue through the same proxy with the selected candidate's headers.
- Frontend HLS recovery continues instead of permanently stopping on transient network/media errors.

## Logos
- Live TV UI always requests `/api/v1/live/channels/{id}/logo` from the same origin.
- Backend first looks in the locally cloned IPTV-org `logos/` directory.
- If a local logo is not available, it fetches the IPTV-org catalogue logo through the compatibility client.
- Browser-side third-party logo host restrictions were removed.

## Search
- Added an in-memory read-optimized search index.
- Bloom filter provides an O(1) negative gate for impossible 3-gram queries.
- Trigram inverted index narrows substring candidates before exact matching.
- Exact matching removes Bloom/index false positives.
- Search covers channel name, country, category, language, TVG name and TVG ID.
- Country/category intersection is applied after candidate generation, preserving exact filter semantics.
- Search facet counts use the same indexed candidate set.
- Database remains the source of truth; index is rebuilt after IPTV synchronization.

## Important operational note
No software can guarantee a third-party broadcaster will never fail. V8 is designed so that the application does not stop merely because Java rejects a certificate, a browser CORS policy blocks the provider, a stream returns a transient error, or one IPTV-org candidate is unavailable. If the upstream provider is genuinely offline, blocked at the network level, or returns no usable media from any candidate, playback can still be impossible.

## Local IPTV checkout
Expected development layout:

    project-root/
      iptv/
        data/
        streams/
        logos/

or configure `livetv.iptv-org.local-repo-path` and `livetv.iptv-org.local-streams-path` explicitly.
