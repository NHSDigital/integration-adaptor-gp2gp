#!/usr/bin/env bash

RED='\033[31m'
NC='\033[0m'

if [ -f "../vars.sh" ]; then
    source ../vars.sh
else
  echo "${RED}ERROR: Missing vars.sh file${NC}"
  exit 1
fi

json_escape () {
    printf '%s' "$1" | python -c 'import json,sys; print(json.dumps(sys.stdin.read()))'
}

mhs_request() {
  curl -i -k -v -X POST \
    -H "Content-Type: application/json" \
    -H "Interaction-Id: $INTERACTION_ID" \
    -H "from-asid: $FROM_ASID" \
    -H "wait-for-response: false" \
    -H "Correlation-Id: $(uuidgen)" \
    -H "ods-code: $GP2GP_RESPONDER_ODS" \
    -d "$REQUEST_BODY" \
    "http://localhost:80"
}