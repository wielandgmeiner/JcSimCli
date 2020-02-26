#!/bin/bash

JAR=../../../build/libs/JcSimCli-1.0-SNAPSHOT-all.jar
PORT=6666
AID=01020304050607080901
CLASS=fr.bmartel.helloworld.HelloWorld
URL="file:///home/wieland/Work/tech/java/javacard/javacard-tutorial/jc101-hello-world/build/classes/main/"

# CLA INS P1 P2
INS_INPUT_BYTES=00a40000
# INS SW1 SW2
INS_RESPONSE_BYTES=a49000

# CLA INS P1 P2 Lc(=0e) Data field (48 ... 21 = "Hello Specter!") Le (32 for SHA256 i.e. 32 bytes)
SHA256_INPUT_BYTES=004200000e48656c6c6f20537065637465722120
# 32 bytes SHA256("Hello Specter!") SW1 SW2
SHA256_OUTPUT_BYTES=a742085d2e645b66d09b7fee4ce9023e007c7c6b46ee7f1ba04efddf5ead5ed19000

echo "---------------------------------------------------------------------------------"
echo " read text representation of APDU commands from stdin and write to stdout"
echo "---------------------------------------------------------------------------------"
java -jar $JAR -a $AID -c $CLASS -u $URL -x <<EOF
$INS_INPUT_BYTES
$SHA256_INPUT_BYTES
EOF

echo "---------------------------------------------------------------------------------"
echo " read text representation of APDU commands from socket and write back to socket"
echo "---------------------------------------------------------------------------------"
java -jar $JAR -p $PORT -a $AID -c $CLASS -u $URL -x &
PID=$!
sleep 1
# open socket
exec 3<>/dev/tcp/localhost/$PORT
echo $INS_INPUT_BYTES >&3
# read from socket
head -1 <&3 | awk '{print $1}' >&1
echo $SHA256_INPUT_BYTES >&3
head -1 <&3 | awk '{print $1}' >&1
kill $PID
exec 3>&-
