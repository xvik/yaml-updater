* Fix multiline parsing: don't always include trailing empty lines (fixes correct comments detection after multiline)
* Fix detection of sequence ([]) and raw object ({}) in list value (don't try to search property in it)

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