#!/usr/bin/env bash

# setup nginx for local development

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
nginxHome=`nginx -V 2>&1 | grep "configure arguments:" | sed 's/[^*]*conf-path=\([^ ]*\)\/nginx\.conf.*/\1/g'`

confDir=sites-enabled

sudo ln -fs $DIR/amiable.conf $nginxHome/$confDir/amiable.conf
sudo ln -fs $DIR/amiable.crt $nginxHome/amiable.crt
sudo ln -fs $DIR/amiable.key $nginxHome/amiable.key
sudo nginx -s stop
sudo nginx
