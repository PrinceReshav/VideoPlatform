# V8.1 Playback Regression Fix

The V8 playback outage was caused by an invalid request header in the IPTV proxy:
`Connection: keep-alive`.

Java `java.net.http.HttpRequest.Builder` rejects `Connection` as a restricted header before any request reaches the upstream IPTV server. This made every proxied playback attempt fail with:
`restricted header name: "Connection"`.

V8.1 removes that header. The JDK manages the connection header itself.

No logo, search-index, filter, candidate-selection, or HLS-rewrite behavior is otherwise changed by this regression fix.
