#!/usr/bin/env bash

source ../common.sh

#Party Key X26-9199246

EHR_EXTRACT=$(cat EhrExtract.xml)
PAYLOAD=$(json_escape "$EHR_EXTRACT")
REQUEST_BODY="{\"payload\":$PAYLOAD}"
INTERACTION_ID=RCMR_IN030000UK06

mhs_request

#curl -i -k -v -X POST \
#-H "Content-Type: application/json" \
#-H "Interaction-Id: RCMR_IN030000UK06" \
#-H "from-asid: $FROM_ASID" \
#-H "wait-for-response: false" \
#-H "Correlation-Id: $(uuidgen)" \
#-H "ods-code: $GP2GP_RESPONDER_ODS" \
#-d "$REQUEST_BODY" \
#"http://localhost:80"
