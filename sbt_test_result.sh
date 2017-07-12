#!/bin/bash

echo "Waiting for runayamel_yvideo_1 docker container to exit"
sudo docker wait runayamel_yvideo_test_1
result=$(sudo docker inspect runayamel_yvideo_test_1 -f "{{ .State.ExitCode }}")
echo "$result"
exit "$result"

