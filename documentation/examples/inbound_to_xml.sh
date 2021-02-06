#!/usr/bin/env bash

DECODED=$(base64 -d $1)
JSON=${DECODED:5}
echo "$JSON" | python -c 'import sys, json; print(json.load(sys.stdin)["payload"])'
