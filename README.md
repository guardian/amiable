# Amiable
Amiable is a web app for monitoring the use of AMIs.
You can access it here:
https://amiable.gutools.co.uk


## How to run Amiable locally
Amiable uses Google Auth. For this reason, we need to run Amiable through an nginx proxy `amiable.local.dev-gutools.co.uk`. 
This can be achieved by running:

```shell script
./script/setup
```
 
 - Setup Amiable configuration.
 A conf file is expected from Amiable.
 The location of that file (as shown in `./sbt`) is: `$HOME/.gu/amiable.local.conf`
 That file must contain all the configuration values that exist in `application.conf`
 
 For example the following values must be set:
 ```
    APPLICATION_SECRET="abcdefghijklmnopqrstuvwxyz"
    PRISM_URL="http://prism.gutools.co.uk"
    HOST="https://amiable.local.dev-gutools.co.uk"
 ```
 In order to setup the auth parameters (eg. `serviceAccountCertPath`),
 please consult someone from the Dev Tools team.
 
 - `./sbt run` open your browser at `https://amiable.local.dev-gutools.co.uk`!
 
### Common problems
 - If when running master you can an error "Could not find a suitable constructor..." it's something wrong with your
 config file - you probably need to add `include "application.conf"` to your `application.local.conf` file.
