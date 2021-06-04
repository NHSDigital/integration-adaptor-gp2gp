#!/usr/bin/env bash

source ../common.sh

PAYLOAD="$(envsubst < payload.xml)"
#PAYLOAD='<xml attr="1"/>'
PAYLOAD=$(json_escape "$PAYLOAD")
ATTACHMENT="$(cat attachment.txt | tr -d " \t\n\r" )"
#ATTACHMENT="asdf123="
REQUEST_BODY="{\"payload\":$PAYLOAD,\"attachments\":[{\"is_base64\":true,\"content_type\":\"application/octet-stream\",\"payload\":\"$ATTACHMENT\",\"description\":\"The attachment\"}]}"
INTERACTION_ID="COPC_IN000001UK01"

mhs_request
