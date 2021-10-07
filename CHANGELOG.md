* [api]
  - Fix multiline parsing: don't always include trailing empty lines (fixes correct comments detection after multiline)
  - Fix object and lists FLOW style support (e.g. [1, 2] and {one: 1, t: 2}): 
     such objects considered as single value and not parsed.
  - Fix update file size and lines count calculation (no trim anymore; affects cli tools report)
  - Add dryRun option for execution simulation (in this case entire merged file is stored in report object)
    Suitable for migration tests.
  - Builder:
    * Delete props and variables methods may be called multiple times (values aggregated)
    * Deprecated envVars: use vars instead
    * Add var method to simplify single variable declaration
* [dropwizard]
  - Add --dry-run option for update simulation. Prints merged config to console.
* [cli]
  - Add --dry-run option for update simulation. Prints merged config to console.

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