Amiable
=======
Amiable is a web app for monitoring the use of AMIs.
You can access it here:
https://amiable.gutools.co.uk


### How to run Amiable locally

Amiable uses Google Auth.
For this reason, in order to set up locally, the following steps must be followed:
 - Enter the following entry in your `/etc/hosts` file:
```
# Amiable
127.0.0.1   amiable.local.dev-gutools.co.uk
```
 
 - Set up nginx.
 In case you don't have nginx you must first install it.
 To do that in Mac run: `brew install nginx` (requires [homebrew](https://brew.sh/))

 To set up nginx simply run the script `./nginx/amiable.sh`
 
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
 config file - you probably need to add include "application.conf" to you application.local.conf file.