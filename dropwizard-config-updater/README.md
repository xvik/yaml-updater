# Dropwizard command for config update

Should be used for updating configuration just after new application version installation
(before startup).

Works with dropwizard 1.x and 2.x.

Arguments and behaviour are almost equivalent to [cli module](../yaml-config-updater-cli)
(full argument names works a bit differently). 

For general workflow and update rules read [root readme](../../../).

### Setup

[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus/dropwizard-config-updater.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/dropwizard-config-updater)

Maven:

```xml
<dependency>
  <groupId>ru.vyarus</groupId>
  <artifactId>dropwizard-config-updater</artifactId>
  <version>1.3.0</version>
</dependency>
```

Gradle:

```groovy
implementation 'ru.vyarus:dropwizard-config-updater:1.3.0'
```


Register bundle:

```java
bootstrap.addBundle(new UpdateConfigBundle());
```

### Usage

```
java -jar yourApp.jar update-config config.yml update.yml
```

This will merge update.yml into config.yml file (creating backup of original file).
*Config.yml may not exist* (initial installation case).

Update file may be a local file, path from classpath or any [URL](https://docs.oracle.com/javase/7/docs/api/java/net/URL.html)
(e.g. to load new config from remote server).

NOTE: classpath case is useful if new configuration was packaged inside application jar. E.g.:

```
java -jar yourApp.jar update-config config.yml /config/default.yml
```

#### Options

```
usage: java -jar project.jar
       update-config [-b] [-d DELETE [DELETE ...]] [-e ENV [ENV ...]]
       [-v] [-s] [-i] [--dry-run] [-h] file update

Update configuration file from new file

positional arguments:
  file                   Path to updating configuration file (might not exist)
  update                 Path to new configuration  file.  Could  also be a
                         classpath path or any URL.

named arguments:
  -b, --no-backup        Don't create  backup  before  configuration update
                         (default: true)
  -d DELETE [DELETE ...], --delete-path DELETE [DELETE ...]
                         Delete properties from  the  current config before
                         update
  -e ENV [ENV ...], --env ENV [ENV ...]
                         Variables to replace  (name=value)  or  path(s) to
                         properties file with  variables  (could  also be a
                         classpath path or any URL)
  -v, --no-validate      Don't   validate   the    resulted   configuration
                         (default: true)
  -s, --non-strict       Don't fail if specified  properties  file does not
                         exists (default: true)
  -i, --verbose          Show debug logs (default: false)
  --dry-run              Test  run  without   file  modification  (default:
                         false)
  -h, --help             show this help message and exit
```

#### Dry run

You can run with `--dry-run` option to test migration: this is complete migration process, but
without actual configuration updating (and no backup creation). Updated file would only be printed into console
for consultation.  

```
java -jar yourApp.jar update-config --dry-run config.yml /config/default.yml
```

#### Delete props

To [delete deprecated property or replace property value](../yaml-config-updater#delete-props)
use `-d` flag (or `--delete-path=`).

For example, to remove `list` sub-tree under `prop1`:

```
java -jar yourApp.jar update-config config.yml update.yml -d prop1.list
```

You can specify multiple properties:

```
java -jar yourApp.jar update-config config.yml update.yml -d prop1.list prop2
```

For full flag name:

```
java -jar yourApp.jar update-config config.yml update.yml --delete-path prop1.list prop2
```

NOTE: you can use both '.' and '/' as level separator ('/' is useful when property name contains dots)

#### Variables

To [specify environment variables](../yaml-config-updater#env-vars) use `-e` (or `--env=`):

```
java -jar yourApp.jar update-config config.yml update.yml -e name=foo
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
java -jar yourApp.jar update-config config.yml update.yml -e name=foo var2=other
```

Other syntax:

```
java -jar yourApp.jar update-config config.yml update.yml --env name=foo var2=other
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
java -jar yourApp.jar update-config config.yml update.yml -e vars.properties
```

Multiple files could be declared. Files and direct variable declaration could be mixed:

```
java -jar yourApp.jar update-config config.yml update.yml -e vars.properties custom=name
```

Variables file could be an url or classpath path (same as with update file):

```
java -jar yourApp.jar update-config config.yml update.yml -e http://mydomian.com/vars.properties
```

By default, error is thrown if specified variables file does not exist.
But you can use `-s` (`--non-strict`) option to ignore not existing files.