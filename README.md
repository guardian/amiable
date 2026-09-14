# Amiable
Amiable is a web app for monitoring the use of AMIs.
You can access it here:
https://amiable.gutools.co.uk


## How to run Amiable locally
### Local setup

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
