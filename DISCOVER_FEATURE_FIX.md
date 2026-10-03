# Discover / Search Interaction Fix

## What was wrong
The DISCOVER element on SearchPage was a non-interactive `<span>`, so it looked like a feature but could not do anything. The topic chips were interactive, but there was no actual discovery experience behind the main DISCOVER control.

## Changes
- Converted DISCOVER into an interactive control.
- Added an expandable discovery panel with three discovery groups:
  - Trending right now
  - Learn something new
  - Watch & relax
- Every discovery item performs a real search by updating the URL query and triggering the existing React Query search flow.
- Added an explicit **Start with search** action that focuses the main search input.
- Added a **Browse ideas** action that opens the discovery panel.
- Added icons, hover/focus states, responsive layout, animation and accessible ARIA attributes.
- Kept the backend/API as the source for actual video results; the discovery UI itself no longer depends on the backend merely to become interactive.

## Backend note
The backend Search Service still requires API Gateway + Elasticsearch + indexed READY/PUBLIC videos for actual result data. This frontend change does not fabricate video results when the catalog is empty or the backend is unavailable.
