### 1.4.2 (2022-06-11)
* Add docker image (#48)

### 1.4.1 (2021-11-24)
* [cli]
  - Fix show version in native binary (-V argument)
  - Change version format to: version (date)

### 1.4.0 (2021-11-20)
* [api]
  - Fix properties recognition: colon must be followed by space or newline
    (could be not quoted string containing colon or even property name with colon)
  - Add quoted property names support with escaped characters
  - Property writing style could change on update according to target file
    (e.g. quoted property may become unquoted or opposite)
* [cli]
  - Add native binaries (lin, win, mac)

### 1.3.3 (2021-11-15)
* [api]
  - Fix validation error after removing last children in container (#11)
    Now, when last container node is removed, container itself being removed 
    (to avoid treating such node as simple property)

### 1.3.2 (2021-11-04)
* [api]
  - Fix whitespace support between property name and colon: such whitespace is removed

### 1.3.1 (2021-10-29)
* [api]
  - Fix NPE appeared for configs specified as filename only 

### 1.3.0 (2021-10-25)
* [api]
  - Fix support for multilines starting from empty line(s) (without multiline marker)
  - Change dry-run config report to better identify it and remove additional new lines (show exact result)
  - Create parent directories when config is created (not existing config case)
  - Re-style configuration not changed report to look like other reports.
  - Do not show warning when config not changes
  - Add backup directory option
  - Avoid using Yaml#serialize for compatibility with older snakeyaml versions (older dropwizard versions)
* [dropwizard]
  - Add --backup-dir option 
* [cli]
  - Add --backup-dir option

### 1.2.0 (2021-10-09)
* [api]
  - Fix multiline parsing: don't always include trailing empty lines (fixes correct comments detection after multiline)
  - Fix object and lists FLOW style support (e.g. [1, 2] and {one: 1, t: 2}): 
     such objects considered as single value and not parsed (taken as strings in both parsers).
  - Fix update file size and lines count calculation (no trim anymore; affects cli tools report)
  - Fix empty files support
  - Add dryRun option for execution simulation (in this case entire merged file is stored in report object)
     Suitable for migration tests.
  - Add YamlUpdater.createTest(String, String) builder for test runs: pre-configures dry run, allows direct usage of 
     classpath or url configs and prints report and migrated file (could be disabled).
     Assumed to be used for production config migration validation in tests.
  - Builder:
    * Props delete and variables methods may be called multiple times (values aggregated)
    * Deprecated envVars: use vars instead
    * Add var method to simplify single variable declaration
    * Add varsFile method to simplify loading properties file from fs, classpath or url
* [dropwizard]
  - Add --dry-run option for update simulation. Prints merged config to console.
  - Add -s or --non-strict option for ignoring not found variable files 
* [cli]
  - Add --dry-run option for update simulation. Prints merged config to console.
  - Add -s or --non-strict option for ignoring not found variable files
  - Fix windows absolute paths support
  - Files also searched in classpath now (not very useful, simply unification)  

### 1.1.0 (2021-08-31)
* [api]
  - Add update report object (returned after update call)
  - Add report printer (for cli modules)
  - Config not replaced if contents are identical after merge (avoid redundant backups)
* [cli]
  - Print update details after update 
* [dropwizard]
  - Fix dropwizard 1.x compatibility
  - Make dropwizard dependency "provided" (to avoid version clashes)
  - Rename `ConfigUpdaterBundle` into `UpdateConfigBundle` to unify naming with command
  - Print update details after update

### 1.0.0 (2021-08-29)
* Initial release