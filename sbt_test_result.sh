#!/bin/bash

sudo docker wait runayamel_yvideo_1
result=$(sudo docker inspect runayamel_yvideo_1 -f "{{ .State.ExitCode }}")
echo "$result"
exit "$result"

