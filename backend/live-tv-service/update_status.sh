#!/bin/bash
page=0
size=50
while true; do
  echo "Fetching page $page"
  response=$(curl -s "http://localhost:8092/api/v1/live/channels?page=$page&size=$size")
  # Check if we got an empty response or error
  if [[ -z "$response" || "$response" == *"error"* ]]; then
    echo "Error fetching page $page, stopping"
    break
  fi
  # Extract IDs
  ids=$(echo "$response" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
  if [[ -z "$ids" ]]; then
    echo "No IDs found on page $page, assuming no more pages"
    break
  fi
  echo "Found $(echo "$ids" | wc -w) channels on page $page"
  for id in $ids; do
    curl -s -X POST "http://localhost:8092/api/v1/live/channels/$id/check" > /dev/null
  done
  # Check if we have reached the last page
  totalPages=$(echo "$response" | grep -o '"totalPages":[0-9]*' | cut -d':' -f2)
  if [[ $page -ge $((totalPages-1)) ]]; then
    echo "Reached last page, sleeping for 5 minutes"
    sleep 300
    page=0
  else
    ((page++))
  fi
done
