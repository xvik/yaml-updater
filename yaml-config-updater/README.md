# Yaml config updater API

Yaml merger API. Requires only two dependencies: slf4j and snakeyaml.

In most cases it might be more convenient to use cli wrappers:

- [cli](../yaml-config-updater-cli)
- [dropwizard](../dropwizard-config-updater)

For general workflow and update rules read [root readme](../../../).

### Setup

[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus/yaml-config-updater.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/yaml-config-updater)

Maven:

```xml
<dependency>
  <groupId>ru.vyarus</groupId>
  <artifactId>yaml-config-updater</artifactId>
  <version>1.3.0</version>
</dependency>
```

Gradle:

```groovy
implementation 'ru.vyarus:yaml-config-updater:1.3.0'
```

### Usage

```java
UpdateReport report = YamlUpdater.create(new File("config.yaml"), new File("update-config.yaml"))
        .backup(true)
        .update()
```

Optionally, returned report could be used for custom output (see `ReportPrinter` class used by both cli modules).

There is a special factory for [migration tests](#testing).

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

NOTE: alternatively, you can use `FileUtils.findExistingFile('/files/config.yml')`
instead, which could load file from local fs, classpath or URL.

#### Options

The following options supported (goes after updater creation):

| Option | Description | Default |
|--------|-------------|---------|
| backup()| Create backup of original config at the same folder| false |
| vars() | Specify variables to replace in update file | - |
| var() | Specify single variable to replace in update file | - |
| varsFile() | Specify properties file to load variables from | - |
| deleteProps() | Specify properties to remove in current config before update | - |
| validateResult() | Perform complete resulted file validation | true |
| listen() | Specify update process listener (mostly for testing) | - |
| dryRun() | Run migration without fs changes (test run) | - |

#### Backup

Backup file is created only if merged file pass validation so in case when update process fails,
backup will never be created (and original config would not be changed).

Backup is disabled by default. To enable it: `.backup(true)`.

#### Variables

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
to prepare variables map and specify it: `.vars(mapOfVars)`.

Single variable could be declared with `.var('name', 'value')`. Also, variables 
could be loaded from properties file: `.varsFile('file path', true)`. File path might be relative or absolute fs path,
classpath or URL. Boolean parameter configures behaviour when file not found (fail or ignore).

All variable methods might be combined and each could be called multiple times (values would be aggregated).

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

#### Testing

It is a very good idea to keep previous configuration somewhere in the project and test it's migration
to the new version (to avoid surprises on prod). 

There is a special factory simplifying testing:

```java
UpdateReport report = YamlUpdater.createTest("config.yaml", "update-config.yaml")
        .update()
```

It accepts string file paths instead of objects. Path might be relative or absolute
file path, classpath path or URL. The most common case should be in referencing files from 
test classpath.

This mode implicitly activates `dryRun()`, but in contrast to pure dry run it:

* Provide simpler configuration (e.g. simpler to specify classpath files)
* Automatically creates temporary file from provided file content (because main api requires File)
* Prints report and migrated file to console after execution (could be disabled with options).

Note that in dry run mode, migrated file content is stored in returned report field: `report.getDryRunResult()`
(because otherwise it would be impossible to retrieve it for validation - original file is not modified, so all 
changes simply discarded).

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

See project tests for usage examples.