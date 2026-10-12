# Amiable
Amiable is a web app for monitoring the use of AMIs.
You can access it here:
https://amiable.gutools.co.uk


## How to run Amiable locally
### Local setup (without docker)

Amiable uses Google Auth. For this reason, we need to run Amiable through an nginx proxy `amiable.local.dev-gutools.co.uk`.
This can be achieved by running:

```shell script
./script/setup-host
```

 - Setup Amiable configuration.
 A conf file is expected from Amiable.
 The location of that file is: `$HOME/.gu/amiable/amiable.local.conf`
 That file must contain all the configuration values that exist in `application.conf`

 For example the following values must be set:
 ```
    APPLICATION_SECRET="abcdefghijklmnopqrstuvwxyz"
    PRISM_URL="https://prism.gutools.co.uk"
    AMIGO_URL="https://amigo.gutools.co.uk"
    HOST="https://amiable.local.dev-gutools.co.uk"
 ```
 In order to setup the auth parameters (eg. `serviceAccountCertPath`),
 please consult someone from the Dev Tools team.

 - `sbt run` open your browser at `https://amiable.local.dev-gutools.co.uk`!

> [!Note]
> #### Prism network access

Amiable requires access to Prism for its core functionality. It retrieves instance, AMI,
account, launch configuration and ownership data from Prism. The server can start without
a working Prism connection but the dashboard and other data-dependent pages will not work.

The environment running `sbt` must be able to reach the configured `PRISM_URL` across the
network. Ensure any required
VPN or private-network access is active.

### Local setup (with docker)
Amiable can also run using Docker Compose. Install Docker Compose and export
`AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` before starting. For temporary
credentials, also export `AWS_SESSION_TOKEN`. These credentials are passed only
to the app-config container. AWS profiles and SSO configuration are not mounted
or used; setting `AWS_PROFILE` alone does not provide credentials.

Build the application image locally, then start the services:

```shell
sbt Docker/publishLocal
docker compose up
```

The `app-config` service downloads the files from
`s3://deploy-tools-dist/deploy/CODE/amiable/conf/` into a Docker-managed volume.
Amiable starts only after the download succeeds and both `amiable.conf` and
`amiable-service-account-cert.json` are present. It mounts that volume read-only
at `/etc/gu/s3-sync` and listens at `http://localhost:9000`.
The S3 configuration still needs appropriate local Google Auth settings; downloading
CODE configuration does not automatically configure local authentication.
The container also needs network access to Prism as described above.

Override defaults using Compose environment variables:

```shell
DIST_BUCKET=another-bucket \
STAGE=MY_STAGE \
IMAGE=amiable:x.y.z-SNAPSHOT \
PORT=9101 \
docker compose up
```

`STACK` defaults to `deploy` and `STAGE` to `CODE`. No downloaded configuration is written to the repository.
The volume survives container shutdown; to remove the containers and downloaded
configuration, run:

```shell
docker compose down --volumes
```

Use this cleanup command before starting again when switching configuration sources,
refreshing configuration, or switching application images. Avoid sharing the same
Compose project between concurrent runs with different settings.


### Debugging
 To attach a debugger, use sbt's built-in flag: `sbt -jvm-debug 1056 run`. Then connect your IDE's remote debugger to port 1056.

### Common problems
 - If when running main you can an error "Could not find a suitable constructor..." it's something wrong with your
 config file - you probably need to add `include "application.conf"` to your `application.local.conf` file.


## CI/CD
CI is configured in TeamCity. It will execute [`./script/ci`](./script/ci).

CD is configured in RiffRaff. The project name is [`tools::amiable`](https://riffraff.gutools.co.uk/deployment/history?projectName=tools%3A%3Aamiable&page=1).

Note, it was also ["amiable"](https://riffraff.gutools.co.uk/deployment/history?projectName=amiable&page=1) at one point too, however was namespaced to "tools::" for consistency with other projects.
The "amiable" project has a block on it to prevent mistakes.

## Testing sending emails

You can browse to `/sendEmail`, and from there you can manually trigger the email job.
