#!/bin/bash

sudo docker wait runayamel_yvideo_1
exit $(sudo docker inspect runayamel_yvideo_1 -f "{{ .State.ExitCode }}")

