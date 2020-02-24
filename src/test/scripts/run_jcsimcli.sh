#!/bin/bash

# CLA INS P1 P2
INS_INPUT_BYTES=00a40000
# INS SW1 SW2
INS_RESPONSE_BYTES=a49000

# CLA INS P1 P2 Lc(=0e) Data field (48 ... = "Hello Specter!") Le (32 for SHA256 i.e. 32 bytes)
SHA256_INPUT_BYTES=004200000e48656c6c6f20537065637465722120
# 32 bytes SHA256("Hello Specter!") SW1 SW2
SHA256_OUTPUT_BYTES=a742085d2e645b66d09b7fee4ce9023e007c7c6b46ee7f1ba04efddf5ead5ed19000

# read APDU commands from stdin
java -jar ../../../build/libs/JcSimCli-1.0-SNAPSHOT-all.jar <<EOF
$INS_INPUT_BYTES
$SHA256_INPUT_BYTES

EOF

# read APDU commands from socket
