#!/bin/bash

exit $(sudo docker inspect runayamel_yvideo_1 -f "{{ .State.ExitCode }}")

