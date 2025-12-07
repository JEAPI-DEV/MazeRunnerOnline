#!/bin/bash

# Script to import Let's Encrypt certificates into a Java Keystore (JKS)
# Usage: ./import_letsencrypt.sh <domain> <keystore_password>

if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <domain> <keystore_password>"
    exit 1
fi

DOMAIN=$1
PASSWORD=$2
LE_PATH="/etc/letsencrypt/live/$DOMAIN"
KEYSTORE_PATH="keystore.jks"

if [ ! -d "$LE_PATH" ]; then
    echo "Error: Let's Encrypt directory for $DOMAIN not found at $LE_PATH"
    exit 1
fi

# Convert to PKCS12
openssl pkcs12 -export \
    -in "$LE_PATH/fullchain.pem" \
    -inkey "$LE_PATH/privkey.pem" \
    -out keystore.p12 \
    -name tomcat \
    -passout "pass:$PASSWORD"

# Import into JKS
keytool -importkeystore \
    -deststorepass "$PASSWORD" \
    -destkeypass "$PASSWORD" \
    -destkeystore "$KEYSTORE_PATH" \
    -srckeystore keystore.p12 \
    -srcstoretype PKCS12 \
    -srcstorepass "$PASSWORD" \
    -alias tomcat

# Clean up
rm keystore.p12

echo "Keystore generated at $KEYSTORE_PATH"
echo "Update server.properties with:"
echo "server.ssl.enabled=true"
echo "server.ssl.keystore=$KEYSTORE_PATH"
echo "server.ssl.password=$PASSWORD"
