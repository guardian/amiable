#!/usr/bin/env bash

# setup nginx for local development

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
nginxHome=`nginx -V 2>&1 | grep "configure arguments:" | sed 's/[^*]*conf-path=\([^ ]*\)\/nginx\.conf.*/\1/g'`

SYSTEM=$(uname -s)
if [ $SYSTEM == "Darwin" ]; then
    # Mac OS X platform
    confDir=servers
elif [ $SYSTEM == "Linux" ]; then
    confDir=sites-enabled
fi

sudo ln -fs $DIR/amiable.conf $nginxHome/sites-enabled/amiable.conf
sudo ln -fs $DIR/amiable.crt $nginxHome/amiable.crt
sudo ln -fs $DIR/amiable.key $nginxHome/amiable.key
sudo nginx -s stop
sudo nginx
