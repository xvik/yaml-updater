# Yaml config updater CLI

Command line interface for yaml configuration updater.

For general workflow and update rules read [root readme](../../../).

### Setup

[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus/yaml-config-updater-cli.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/yaml-config-updater-cli)

Delivered as:

* Fat jar (all dependencies inside one runnable jar)
* Native binary for linux, mac and windows
* Docker image (alpine + native linux binary)

First two are [attached to release](https://github.com/xvik/yaml-updater/releases/tag/1.4.2).
Docker image uploaded into [github repository](https://github.com/xvik/yaml-updater/pkgs/container/yaml-updater)

#### Fat jar

Requires installed java 8 or above.

Jar could be downloaded from [github release](https://github.com/xvik/yaml-updater/releases/tag/1.4.2)
or from [maven central](https://repo1.maven.org/maven2/ru/vyarus/yaml-config-updater-cli/1.4.2/) (classifier `all`)
(the latter is signed and so might be considered as more secure option, but jars are the same).

Example usage:

```
java -jar yaml-updater.jar --version
```

```
java -jar yaml-updater.jar config.yml update.yml
```

##### Custom packaging

If you want to package it differently, not shadowed version is also available (with transitive dependencies):

```groovy
implementation 'ru.vyarus:yaml-config-updater-cli:1.4.2'
```

You can build your own fat jar with it.

#### Native binaries

Native binaries are attached to [github release](https://github.com/xvik/yaml-updater/releases/tag/1.4.2).
They are build with [graalvm native image](https://www.graalvm.org/22.1/reference-manual/native-image/).

##### Windows

On windows, requires Visual C++ redistributable ([download link](https://www.microsoft.com/en-gb/download/confirmation.aspx?id=48145)).
You might already have it installed (many windows apps (especially games) require it).
When not installed error message will be shown about missed dll.

WARNING: Windows defender or other antivirus software could detect trojans. This is a [known
problem for grallvm](https://github.com/oracle/graal/issues/1752). I can't do anything with it: native binary is build automatically with 
[github action](https://github.com/xvik/yaml-updater/blob/master/.github/workflows/binaries.yml) on release and
does not contain any treats.

```
yaml-updater.exe --version
```

```
yaml-updater.exe config.yml update.yml
```

##### Linux, Mac

On linux and mac glibc is required. On most distributions it is already installed. 

You'll need to set executable flag (github drops it when attaches to release):

```
chmod +x yaml-updater-linux-amd64
```

or 

```
chmod +x yaml-updater-mac-amd64
```

Also, it would be simpler to rename file:

```
mv yaml-updater-linux-amd64 yaml-updater
```

Execution:

```
./yaml-updater --version
```

```
./yaml-updater config.yml update.yml
```

#### Docker

Assuming [docker installed](https://docs.docker.com/engine/install/)

Docker image uploaded to [github repository](https://github.com/xvik/yaml-updater/pkgs/container/yaml-updater)

Image build with [alpine(+glibc) and linux binary](https://github.com/xvik/yaml-updater/blob/master/Dockerfile) and weights only 22mb

Test:

```
docker run -it --rm ghcr.io/xvik/yaml-updater:1.4.2 --version
```

(`--rm` flag used to remove container after execution)

For actual migration, you'll need to mount local folder containing yaml files:
for example, suppose we have both in the current directory

```
docker run -it --rm -v $(pwd):/tmp/data ghcr.io/xvik/yaml-updater:1.4.2 /tmp/data/config.yml /tmp/data/update.yml
```

Here both files (`current.yml` and `update.yml`) are in the current directory, mounted to docker.
After execution `current.yml` would be updated.

You can put a full path to local folder instead of `$(pwd)` (especially on windows):

```
docker run -it --rm -v "C:/data":/tmp/data ghcr.io/xvik/yaml-updater:1.4.2 /tmp/data/config.yml /tmp/data/update.yml
```

### Usage

You can use whatever way you want, but, for simplicity, examples below are shown with windows binary
(in all cases parameters are the same).

```
yaml-updater.exe config.yml update.yml
```

This will merge update.yml into config.yml file (including creation of backup for original file). 
*Config.yml may not exist* (initial installation case).

Update file may be a local file or any [URL](https://docs.oracle.com/javase/7/docs/api/java/net/URL.html)
(e.g. to load new config from remote server).

NOTE: longer version with command name will also work (as shown in help):

```
yaml-updater.exe update-config config.yml update.yml
```

#### Options

```
Usage: [-bhisvV] [--dry-run] [--backup-dir=BACKUPDIR]
                     [-d=DELETE...]... [-e=ENV...]... CONFIG UPDATE
Update yaml configuration file from new file
      CONFIG          Path to updating configuration file (might not exist)
      UPDATE          Path to new configuration file or any URL
  -b, --no-backup     Don't create backup before configuration update
      --backup-dir=BACKUPDIR
                      Directory to store backup in
  -d, --delete-path=DELETE...
                      Delete properties from the current config before update
      --dry-run       Test run without file modification
  -e, --env=ENV...    Variables to replace (name=value) or path(s) to
                        properties file with variables
  -h, --help          Show this help message and exit.
  -i, --verbose       Show debug logs
  -s, --non-strict    Don't fail if specified properties file does not exists
  -v, --no-validate   Don't validate the resulted configuration
  -V, --version       Print version information and exit.
```

Two notions of options supported: short flags and full names. 
short flags might be aggregated (`-bv` == `-b -v`).

NOTE: it is not highlighted, but you can also use classpath files for update file or variable files.
In most cases this, of course, is useless (as tool supposed to be used standalone).

#### Dry run

You can run with `--dry-run` option to test migration: this is complete migration process, but
without actual configuration updating (and no backup creation). Updated file would only be printed into console
for consultation.

```
yaml-updater.exe --dry-run config.yml update.yml
```

#### Delete props

To [delete deprecated property or replace property value](../yaml-config-updater#delete-props)
use `-d` flag (or `--delete-path=`).

For example, to remove `list` sub-tree under `prop1`:

```
yaml-updater.exe config.yml update.yml -d prop1.list
```

You can specify multiple properties:

```
yaml-updater.exe config.yml update.yml -d prop1.list prop2 
```

For full flag name:

```
yaml-updater.exe config.yml update.yml --delete-path=prop1.list --delete-path=prop2
```

NOTE: you can use both '.' and '/' as level separator ('/' is useful when property name contains dots)

#### Variables

To [specify environment variables](../yaml-config-updater#env-vars) use `-e` (or `--env=`):

```
yaml-updater.exe config.yml update.yml -e name=foo
```

If update.yaml contains:

```yaml
prop: #{name}
```

then it would be replaced as:

```yaml
prop: foo
```

NOTE: not specified variables are not replaced!

Multiple variables:

```
yaml-updater.exe config.yml update.yml -e name=foo var2=other
```

Other syntax:

```
yaml-updater.exe config.yml update.yml --env=name=foo --env=var2=other
```

##### Defaults

By default, all system environment variables (`System.getenv()`) are available, 
so if on target host variables already declared in environment, it would be replaced automatically.

##### Variables file

It is also possible to use properties files for variables definition:

vars.properties

```properties
name=foo
var2=other
```

```
yaml-updater.exe config.yml update.yml -e vars.properties
```

Multiple files could be declared. Files and direct variables declaration could be mixed:

```
yaml-updater.exe config.yml update.yml -e vars.properties custom=name
```

Variables file could be an url (same as with update file):

```
yaml-updater.exe config.yml update.yml -e http://mydomian.com/vars.properties
```

By default, error is thrown if specified variables file does not exist.
But you can use `-s` (`--non-strict`) option to ignore not existing files.