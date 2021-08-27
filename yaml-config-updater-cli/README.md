# Yaml config updater CLI

Command line interface for yaml configuration updater.

For general workflow and update rules read [root readme](../).

### Setup

[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus/yaml-config-updater.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/yaml-config-updater)

[Download jar from maven central](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/yaml-config-updater) (classifier `all`)

It will include all required dependencies (shadowjar).

#### Custom packaging

If you want to package it differently, not shadowed version is also available:

```groovy
implementation 'ru.vyarus:yaml-config-updater-cli:1.0.0'
```

Use it for required packaging.

### Usage

You will need java installed on target environment.

```
java -jar yaml-config-updater-cli.jar config.yml update.yml
```

This will merge update.yml into config.yml file (creating backup of original file). 
*Config.yml may not exist* (initial installation case).

Update file may be a local file or any [URL](https://docs.oracle.com/javase/7/docs/api/java/net/URL.html)
(e.g. to load new config from remote server).

#### Options

```
Usage: [-bhivV] [-d=DELETE...]... [-e=ENV...]... CONFIG UPDATE
Update yaml configuration file from new file
      CONFIG          Path to updating configuration file (might not exist)
      UPDATE          Path to new configuration file or any URL
  -b, --no-backup     Don't create backup before configuration update
  -d, --delete-path=DELETE...
                      Delete properties from the current config before update
  -e, --env=ENV...    Variables to replace (name=value) or path(s) to
                        properties file with variables
  -h, --help          Show this help message and exit.
  -i, --verbose       Show debug logs
  -v, --no-validate   Don't validate the resulted configuration
  -V, --version       Print version information and exit.
```

Two notions of options supported: short flags and full names. 
short flags might be aggregated (`-bv` == `-b -v`).

#### Delete props

To [delete deprecated property or replace property value](../yaml-config-updater#delete-props)
use `-d` flag (or `--delete-path=`).

For example, to remove `list` sub-tree under `prop1`:

```
java -jar yaml-config-updater-cli.jar -d prop1.list config.yml update.yml
```

You can specify multiple properties:

```
java -jar yaml-config-updater-cli.jar -d prop1.list prop2 config.yml update.yml
```

For full flag name:

```
java -jar yaml-config-updater-cli.jar --delete-path=prop1.list --delete-path=prop2 config.yml update.yml
```

NOTE: you can use both '.' and '/' as level separator ('/' is useful when property name contains dots)

#### Variables

To [specify environment variables](../yaml-config-updater#env-vars) use `-e` (or `--env=`):

```
java -jar yaml-config-updater-cli.jar -e name=foo config.yml update.yml
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
java -jar yaml-config-updater-cli.jar -e name=foo var2=other config.yml update.yml
```

Other syntax:

```
java -jar yaml-config-updater-cli.jar --env=name=foo --env=var2=other config.yml update.yml
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
java -jar yaml-config-updater-cli.jar -e vars.properties config.yml update.yml
```

Multiple files could be declared. Files and direct variables declaration could be mixed:

```
java -jar yaml-config-updater-cli.jar -e vars.properties custom=name config.yml update.yml
```

Variables file could be an url (same as with update file):

```
java -jar yaml-config-updater-cli.jar -e http://mydomian.com/vars.properties config.yml update.yml
```