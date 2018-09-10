#!/bin/bash

echo "Waiting for runayamel_yvideo_1 docker container to exit"
sudo docker wait yvideo_yvideo_test_1
result=$(sudo docker inspect yvideo_yvideo_test_1 -f "{{ .State.ExitCode }}")
echo "ExitCode: [$result]"
exit "$result"

