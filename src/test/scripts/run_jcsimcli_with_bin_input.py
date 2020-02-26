#!/usr/bin/env python3

import socket
import subprocess
import time
JAR="../../../build/libs/JcSimCli-1.0-SNAPSHOT-all.jar"
PORT="6666"
AID="01020304050607080901"
CLASS="fr.bmartel.helloworld.HelloWorld"
URL="file:///home/wieland/Work/tech/java/javacard/javacard-tutorial/jc101-hello-world/build/classes/main/"

# CLA INS P1 P2
INS_INPUT_BYTES=b'\x00\xa4\x00\x00'
INS_INPUT_BYTES_HEX='00a40000'
# INS SW1 SW2
INS_RESPONSE_BYTES=b'\xa4\x90\x00'
INS_RESPONSE_BYTES_HEX='a49000'

# CLA INS P1 P2 Lc(=0e) Data field (48 ... 21 = "Hello Specter!") Le (32 for SHA256 i.e. 32 bytes)
SHA256_INPUT_BYTES=b'\x00\x42\x00\x00\x0e\x48\x65\x6c\x6c\x6f\x20\x53\x70\x65\x63\x74\x65\x72\x21\x20'
SHA256_INPUT_BYTES_HEX='004200000e48656c6c6f20537065637465722120'
# 32 bytes SHA256("Hello Specter!") SW1 SW2
SHA256_OUTPUT_BYTES=b'\xa7\x42\x08\x5d\x2e\x64\x5b\x66\xd0\x9b\x7f\xee\x4c\xe9\x02\x3e\x00\x7c\x7c\x6b\x46\xee\x7f\x1b\xa0\x4e\xfd\xdf\x5e\xad\x5e\xd1\x90\x00'
SHA256_OUTPUT_BYTES_HEX='a742085d2e645b66d09b7fee4ce9023e007c7c6b46ee7f1ba04efddf5ead5ed19000'

HOST = '127.0.0.1'
with subprocess.Popen(["java", "-jar", JAR, "-p", PORT, "-a", AID, "-c", CLASS, "-u", URL]) as proc:

    time.sleep(1)

    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.connect((HOST, int(PORT)))
        s.sendall(INS_INPUT_BYTES)
        data = s.recv(len(INS_RESPONSE_BYTES))
        assert data is not None
        assert len(data) == 3
        assert data == INS_RESPONSE_BYTES
        s.sendall(SHA256_INPUT_BYTES)
        data = s.recv(len(SHA256_OUTPUT_BYTES))
        assert data is not None
        assert len(data) == 34 # 32 sha256 + 2 for the SW1 and SW2
        assert data == SHA256_OUTPUT_BYTES
        s.close()

    proc.kill()
