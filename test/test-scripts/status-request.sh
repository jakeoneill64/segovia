#!/bin/bash

# this is a dev script to manually determine API behaviour.

PAYMENT_PROVIDER_URI='192.168.0.18:7902'
TOKEN=$1
CONVERSATION_ID=$2

curl -H "Content-Type: application/json" \
       -H "Authorization: Bearer ${TOKEN}" \
       -X GET \
        "http://${PAYMENT_PROVIDER_URI}/status/${CONVERSATION_ID}"
