# Dropwizard command for config update

Might be used for merging configuration just after new application version installation
(before startup).

Arguments and behaviour is almost equivalent to [cli module](../yaml-config-updater-cli)
(full argument names works a bit differently). 

For general workflow and update rules read [root readme](../).

### Setup

[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus/yaml-config-updater.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/yaml-config-updater)

Maven:

```xml
<dependency>
  <groupId>ru.vyarus</groupId>
  <artifactId>dropwizard-config-updater</artifactId>
  <version>1.0.0</version>
</dependency>
```

Gradle:

```groovy
implementation 'ru.vyarus:dropwizard-config-updater:1.0.0'
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

NOTE: classpath case is useful if new configuration is packaged inside application jar

#### Options

```
usage: java -jar project.jar update-config [-b] [-d DELETE [DELETE ...]]
                             [-e ENV [ENV ...]] [-v] [-i] [-h] file update

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
  -i, --verbose          Show debug logs (default: false)
  -h, --help             show this help message and exit
```

#### Delete props

To [delete deprecated property or replace property value](../yaml-config-updater#delete-props)
use `-d` flag (or `--delete-path=`).

For example, to remove `list` sub-tree under `prop1`:

```
java -jar yourApp.jar update-config -d prop1.list config.yml update.yml
```

You can specify multiple properties:

```
java -jar yourApp.jar update-config -d prop1.list prop2 config.yml update.yml
```

For full flag name:

```
java -jar yourApp.jar update-config --delete-path prop1.list prop2 config.yml update.yml
```

NOTE: you can use both '.' and '/' as level separator ('/' is useful when property name contains dots)

#### Variables

To [specify environment variables](../yaml-config-updater#env-vars) use `-e` (or `--env=`):

```
java -jar yourApp.jar update-config -e name=foo config.yml update.yml
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
java -jar yourApp.jar update-config -e name=foo var2=other config.yml update.yml
```

Other syntax:

```
java -jar yourApp.jar update-config --env name=foo var2=other config.yml update.yml
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
java -jar yourApp.jar update-config -e vars.properties config.yml update.yml
```

Multiple files could be declared. Files and direct variable declaration could be mixed:

```
java -jar yourApp.jar update-config -e vars.properties custom=name config.yml update.yml
```

Variables file could be an url or classpath path (same as with update file):

```
java -jar yourApp.jar update-config -e http://mydomian.com/vars.properties config.yml update.yml
```