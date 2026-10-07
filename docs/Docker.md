To build and test locally:

run 
```
script/ci
```

Check it worked:
```
docker images amiable
```

Inspect:
```
docker run --rm -it --entrypoint /bin/sh guardian/amiable:latest 
```
Note that the entrypoint is /opt/docker/bin/amiable

Get config:
```
mkdir s3-sync
aws s3 sync s3://deploy-tools-dist/deploy/CODE/amiable/conf/ s3-sync/
```

Run it locally (overriding the entrypoint for ease):
```
docker run \
  --rm \
  -p 9000:9000 \
  -v "$PWD/s3-sync:/etc/gu/s3-sync:ro" \
  -e APPLICATION_SECRET="$(openssl rand -hex 32)" \
  -e PRISM_URL="$(openssl rand -hex 32)" \
  -e AMIGO_URL="$(openssl rand -hex 32)" \
  -e HOST="$(openssl rand -hex 32)" \
  -e AUTH_DOMAIN="$(openssl rand -hex 32)" \
  -e GOOGLE_SERVICE_ACCOUNT_CERT_PATH="/amiable/amiable-service-account-cert.json" \
  -e GOOGLE_CLIENT_ID="$(openssl rand -hex 32)" \
  -e GOOGLE_CLIENT_SECRET="$(openssl rand -hex 32)" \
  -e GOOGLE_2FA_USER="$(openssl rand -hex 32)" \
  -e 2FA_GROUP_ID="$(openssl rand -hex 32)" \
  -e DEPARTMENT_GROUP_ID="$(openssl rand -hex 32)" \
  -e STAGE="$(openssl rand -hex 32)" \
  -e MAIL_ADDRESS="$(openssl rand -hex 32)" \
  -e OWNER_NOTIFICATION_CRON="$(openssl rand -hex 32)" \
  -it --entrypoint /bin/sh \
  guardian/amiable:latest
```
then invoke `./bin/amiable` to run the app.

