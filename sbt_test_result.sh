#!/bin/bash

container="yvideo_yvideo_test_1"

echo "Waiting for $container docker container to exit"
sudo docker wait $container
result=$(docker inspect $container -f "{{ .State.ExitCode }}")

if [[ -z "$(which nc)" ]]; then
    apt-get install netcat -y
fi
# upload logs to termbin
# logs seem to be truncated on travis-ci
echo "SBT TEST LOGS: $(docker logs $container | nc termbin.com 9999)"

echo "ExitCode: [$result]"
exit "$result"

