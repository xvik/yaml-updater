# Yaml config updater CLI

Command line interface for yaml configuration updater.

For general workflow and update rules read [root readme](../../../).

### Setup

[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus/yaml-config-updater-cli.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/yaml-config-updater-cli)

There are native binaries [attached to release](https://github.com/xvik/yaml-updater/releases/tag/1.4.2) for windows, mac and linux.
Also, shadowed (all dependencies included) jar attached to release.

Signed version of shadowed is also [published to maven central](https://repo1.maven.org/maven2/ru/vyarus/yaml-config-updater-cli/1.4.2/) (classifier `all`)
(might be more preferable for security reasons).

#### Custom packaging

If you want to package it differently, not shadowed version is also available:

```groovy
implementation 'ru.vyarus:yaml-config-updater-cli:1.4.2'
```

Use it for required packaging.

### Usage

For native binary (for example, on windows): 

```
yaml-updater.exe config.yml update.yml
```

If you don't want to use native version then you'll need java installed on target environment.

```
java -jar yaml-updater.jar config.yml update.yml
```

This will merge update.yml into config.yml file (creating backup of original file). 
*Config.yml may not exist* (initial installation case).

Update file may be a local file or any [URL](https://docs.oracle.com/javase/7/docs/api/java/net/URL.html)
(e.g. to load new config from remote server).

NOTE: longer version with command name will also work (as shown in help):

```
java -jar yaml-updater.jar update-config config.yml update.yml
```

IMPORTANT: Below I'll show shorter command versions for native binaries. If you use jar instead of native
version then prepend `java -jar`

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