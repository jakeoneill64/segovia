#!/bin/bash

ID,Recipient,Amount,Currency
aaaaaaaa,254999999999,10,KES

curl -X POST

msisdn	M	string	Recipient phone number.
amount	M	decimal	Amount to pay.
currency	M	string	ISO-4217 currency code of amount.
reference	O	string	Client-supplied identifier for this payment.
url	O	string	URL to send callback request with payment result.