#!/bin/bash

container="yvideo_yvideo_test_1"

echo "Waiting for $container docker container to exit"
sudo docker wait $container
result=$(docker inspect $container -f "{{ .State.ExitCode }}")

if [[ -n "$(which nc)" ]]; then
    docker logs $container | nc termbin.com 9999
else
    docker logs $container
fi

echo "ExitCode: [$result]"
exit "$result"

