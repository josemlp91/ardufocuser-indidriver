#!/bin/bash
java  -Djava.library.path=/usr/lib/jni -Djava.library.path=/usr/lib/rxtx  -jar ./I4JServer/dist/I4JServer.jar -add=/home/josemlp/workspace/proyectos/I4JArdufocuserDriver/dist/I4JArdufocuserDriver.jar &
websockify localhost:9999 localhost:7624
