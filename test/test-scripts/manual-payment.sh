#!/bin/bash

# this is a dev script to manually determine API behaviour.

PAYMENT_PROVIDER_URI='192.168.0.18:7902'
SAMPLE_PAYMENTS=$(sed '1d' ../sample-input.csv)
CALLBACK_URL=http://192.168.0.12:8080/callback

ACCOUNT_ID=SEGOVIA
KEY=0aVp83wuFp6wjvQ3

authenticate(){
  curl -H "Content-Type: application/json" \
       -H "Api-Key: ${KEY}" \
           -X POST \
           -d "{\"account\": \"${ACCOUNT_ID}\"}" \
           "http://${PAYMENT_PROVIDER_URI}/auth"
}

TOKEN=$(authenticate | python3 -c "import sys, json; print(json.load(sys.stdin)['token'])")

echo authentication token: $TOKEN

for line in $SAMPLE_PAYMENTS
do
  COLUMNS=($(echo "${line}" | awk -F',' '{print $1,$2,$3,$4}'))
  ID=${COLUMNS[0]}
  RECIPIENT=${COLUMNS[1]}
  AMOUNT=${COLUMNS[2]}
  CURRENCY=${COLUMNS[3]}

  BODY="{\"msisdn\":\"${RECIPIENT}\",\"amount\":${AMOUNT},\"currency\":\"${CURRENCY}\",\"url\":\"${CALLBACK_URL}\"}"

  curl -H "Content-Type: application/json" \
       -H "Authorization: Bearer ${TOKEN}" \
       -X POST \
       -d $BODY \
       -v \
        "http://${PAYMENT_PROVIDER_URI}/pay"

done