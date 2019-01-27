#!/bin/bash

# This script is meant for deploying from travis after a successful build
chmod 600 private_key

if [[ "$BRANCH" == "master" ]]; then
    YVIDEO_VERSION="--production"
    SERVER=$PROD_SERVER
else
    YVIDEO_VERSION="--beta"
    SERVER=$BETA_SERVER
fi

PREP_ENV="source /var/yvideo/yvideo-conf/yvideo_env && cd /var/yvideo/yvideo-deploy && git checkout -- . && git checkout master && git pull"
COMMAND="./setup_yvideo.sh $YVIDEO_VERSION --remove --services=v && sleep 20 && ./setup_yvideo.sh $YVIDEO_VERSION --services=v --build --nc && date >> deploy.log"

echo $PREP_ENV "&&" $COMMAND
ssh -i private_key -o StrictHostKeyChecking=no $SSH_USER@$SERVER "$PREP_ENV && $COMMAND"
