# Yaml config updater API

Yaml merger API. Requires only two dependencies: slf4j and snakeyaml.

In most cases it might be more convenient to use cli wrappers:

- [cli](../yaml-config-updater-cli)
- [dropwizard](../dropwizard-config-updater)

For general workflow and update rules read [root readme](../).

### Setup

[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus/yaml-config-updater.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/yaml-config-updater)

Maven:

```xml
<dependency>
  <groupId>ru.vyarus</groupId>
  <artifactId>yaml-config-updater</artifactId>
  <version>1.0.0</version>
</dependency>
```

Gradle:

```groovy
implementation 'ru.vyarus:yaml-config-updater:1.0.0'
```

##### Snapshots

<details>
      <summary>Snapshots may be used through JitPack</summary>

* Go to [JitPack project page](https://jitpack.io/#xvik/yaml-config-updater)
* Select `Commits` section and click `Get it` on commit you want to use (top one - the most recent)
* Follow displayed instruction: add repository and change dependency (NOTE: due to JitPack convention artifact group will be different)


For gradle use `buildscript` block with required commit hash as version:

```groovy
buildscript {
    repositories {
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        classpath 'ru.vyarus:yaml-config-updater:master-SNAPSHOT'
    }
}
apply plugin: 'ru.vyarus.quality'
```

</details>

### Usage

```java
YamlUpdater.create(new File("config.yaml"), new File("update-config.yaml"))
        .backup(true)
        .update()
```

#### Source files

`YamlUpdater.create(..)` specifies original config (to be updated) and new config (to update from).
Current config may not exist: in this case update config simply copied.

Update file may be specified as `InputStream` to allow updating from classpath or remote host.

For example, to update from file in classpath:

```java
YamlUpdater.create(
        new File("config.yml"),
        getClass().getResourceAsStream('/files/config.yml'))
        .update()
```

#### Options

The following options supported (goes after updater creation):

| Option | Description | Default |
|--------|-------------|---------|
| backup()| Create backup of original config at the same folder| true |
| envVars() | Specify variables to replace in update file | - |
| deleteProps() | Specify properties to remove in current config before update | - |
| validateResult() | Perform complete resulted file validation | true |
| listen() | Specify update process listener (mostly for testing) | - |

#### Backup

Backup file is created only if merged file pass validation so in case when update process fails,
backup will never be created (and original config would not be changed).

Backup is enabled by default. To disable it: `.backup(false)`.

#### Env vars

Configuration might contain environment-specific variables, making raw update file
a "template" for creation of real environment-specific config.

The following syntax is used: `#{name}`.

Such syntax used instead of more common `${..}` because:

- Config may rely on environment variables and so will collide with replacing variables
- Such syntax recognized as comment and so not processed value would be empty value after parsing.

```yaml
obj:
  some: #{name}
```

IMPORTANT: only known placeholders would be replaced (unknown variables will remain unprocessed)

By default, no variables specified! So if you need variables support, you need
to prepare variables map and specify it: `.envVars(mapOfVars)`.

NOTE: cli modules use environment by default and supports loading properties 
files as variable sources

#### Delete props

You may need to delete existing properties if:

1. Property no longer used (as merger does not remove anything automatically)
2. Property value need to be replaced with default from new file (e.g. to refresh list value, not merged automatically)

Example file:

```yaml
prop1: 1
sub:
  prop: 2
  list:
    - prop:
        subprop: 1

list:
  - one
  - two
```

Property is identified with "yaml path":

- `prop1` 
- `sub/prop`
- `list[0]` or `list[1]`

Rules:

- Levels separated with `/` because dot might be used in property name. BUT dots still supported, so
    if you write `sub.prop` - it will correctly remove. 
- List items identified with `[n]` where n counts from 0.
- Path might be of any length: `sub/list[0]/prop/subprop` (any amount of sub-levels, including lists)
- If specified property not found - not an error (process will continue)

#### Result validation

After merging, resulted file is always read by snakeyaml to guarantee yaml correctness.

Then, all values from old file checked to be preserved and the same for new values.

Normally, this validation should not be disabled, but in case of probable validation
bugs, you can disable it with `.validateResult(false)`.

#### Listener

NOTE: This is an internal API, providing access to internal data models during
update process. It is mainly intended to be used for testing purposes. Incorrect
usage may lead to errors (as you can incorrectly modify yaml tree models).

In general, this API might be used for manual merge result modification. But int this case
you'll have to disable validation.

Listener registered as `.listen(new MyListener())`. Use `UpdateListenerAdapter` as base class
to avoid implementing all methods.

##### Tree models

There are two tree models used internally:

- `CmtTree` (`CmtNode`) - comments parser tree (preserving file structure)
- `StructTree` (`StructNode`) - snakeyaml (structurally correct) tree (without comments, but with correctly parsed values)

Both trees available for each file. Tree APIs are unified. Trees themselves are also unified (they would have the 
same structure - it is validated internally). The only difference is comments tree might have additional not at the end - trailing comment.

Both trees follows node == line. E.g.

```yaml
prop:         # node 1
  sub:        # child of node 1 (node 2)
    val: 1    # child of node 2
```

The only exception is object lists where two situations possible:

```yaml
list:
  - prop: 1
```

and

```yaml
list:
  -
    prop: 1
```

Tree model would always contain grouping node for item object (with special marker to differentiate these cases):

```
list node
 - list item (wrapper) node
   - item properties
```

Such approach is important to aggregate item objects (easier to deal with).

Both classes `CmtTree` and `StructTree` has overridden toString, producing
"technical" tree representation, suitable for debug.

##### Merged tree modification:

Implement `UpdateListener#merged()` method and modify passed `CmtTree`.
This method is called just before writing tree to file and so all changes
would be applied.

Don't forget to disable validation (as your changes most likley produce validation errors).
Even with disabled validation, written file would be read with snakeyaml to guarantee
yaml correctness.