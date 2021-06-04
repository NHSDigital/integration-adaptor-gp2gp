#!/usr/bin/env bash

source ../common.sh

#Party Key X26-9199246

EHR_EXTRACT="$(envsubst < EhrExtract.xml)"
PAYLOAD=$(json_escape "$EHR_EXTRACT")
REQUEST_BODY="{\"payload\":$PAYLOAD}"
INTERACTION_ID=RCMR_IN030000UK06

mhs_request
