# Yaml config updater
[![License](https://img.shields.io/badge/license-MIT-blue.svg?style=flat)](http://www.opensource.org/licenses/MIT)
[![CI](https://github.com/xvik/yaml-updater/actions/workflows/CI.yml/badge.svg)](https://github.com/xvik/yaml-updater/actions/workflows/CI.yml)
[![Appveyor build status](https://ci.appveyor.com/api/projects/status/github/xvik/yaml-updater?svg=true)](https://ci.appveyor.com/project/xvik/yaml-updater)
[![codecov](https://codecov.io/gh/xvik/yaml-updater/branch/master/graph/badge.svg)](https://codecov.io/gh/xvik/yaml-updater)

Support: [Discussions](https://github.com/xvik/yaml-updater/discussions) | [Gitter chat](https://gitter.im/xvik/yaml-updater)

### About

Merges yaml configuration files, preserving comments and whitespaces. Assumed to be used for microservice configuration
updates.

By default, targets the most common use-case: add all new properties, without removing or changing existing values.
Lists not merged because list is a value and all current values must remain.

Comments preserved using custom (simple) yaml parser. [Snakeyaml](https://bitbucket.org/asomov/snakeyaml/wiki/Documentation) 
parser used for source files validation, comments parser self-validation (compares parse trees)
and result validation.

Due to complete validation, merged file correctness is guaranteed.

Supports:

* Multiline values (all [syntax variations](https://yaml-multiline.info))
* [Reformatting](#merge-rules) (changed paddings in new config in both directions)
* Properties reordering according to new config
* [Variables replacement](#variables) in new config before merge (environment-specific config adoption)
* [Current values remove](yaml-config-updater#delete-props) (e.g. deprecated values or for value replacement)
* [Object list items update](#lists-matching-logic) (lists not merged, but new properties could be added to list items)
* [Backup](yaml-config-updater#backup) current configuration (only if content changes)

IMPORTANT: this is not a general-purpose yaml merge tool because yaml features like multiple documents in
one file and object references are not supported (only common subset of features, used in configs)

### Example

Original config:

```yaml
# top comment

# something
prop:
  one: 1

  two: 2

lists:

  # sub comment
  list:
    - one
    - two

  obj:
    - one: 1
      two: 2

large: multi-line
  value

# trailing comment
```

Update file:

```yaml
# changed comment

# something else
prop:
  one: 3
  two: 3
  three: 3                              # new property

lists:

  # changed sub comment
  list:                                 # padding changed
      - two                             # order changed (ignored)
      - one
      - three                           # new value ignored

  obj:
    - two: 2
      one: 1
      three: 3                        # new value

large: multi-line
  value

# changed trailing comment
```

Merge result:

```yaml
# changed comment

# something else
prop:
  one: 1

  two: 2
  three: 3                              # new property

lists:

  # changed sub comment
  list:
      - one
      - two

  obj:
    - two: 2
      one: 1
      three: 3                        # new value

large: multi-line
  value

# changed trailing comment
```

Merge report (shown by cli modules):

```
Configuration: /var/somewhere/config.yml (185 bytes, 23 lines)
Updated from source of 497 bytes, 25 lines
Resulted in 351 bytes, 25 lines

	Added from new file:
		prop/three                               7  | three: 3                              # new property
		lists/obj[0]/three                       20 | three: 3                        # new value
```

### Setup

[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus/yaml-config-updater.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/yaml-config-updater)

Could be used as:

* [Library](yaml-config-updater) through API
* [Command line util](yaml-config-updater-cli) (jar)
* [Dropwizard plugin](dropwizard-config-updater) (same arguments as in cli module)

Read exact module's readme for setup instructions.

### Merge rules

* All values from current configuration preserved 
* All new properties added
* Order of properties from update file used (current config properties could be re-ordered)
* For existing values, top comments updated from new config (if property exists in new config).
  - If new property does not contain any comment - old comment is preserved
  - In-line comments (after value) not updated (and so may be used for persistent marks)
  - Trailing comment at the end of file preserved as-is (not updated)
* Paddings applied from update file (original config could be re-formatted)
  - Padding change works in both directions (reduce or enlarge)
* Lists not merged
  - Object list items could be updated with new properties (if matched item found)
  - Dash style for object item could change in both directions: empty dash (property on new line) or property on the same line with dash

<table>
<tr>
<th>Case</th>
<th>Current config</th>
<th>Update file</th>
<th>Merge result</th>
</tr>
<tr>
<td>
New properties added
</td>
<td>
   <pre lang="yaml">
one: 1
two: 2
   </pre>
</td>
<td>
  <pre lang="yaml">
one: 3
two: 3
three: 3
  </pre>
</td>
<td>
  <pre lang="yaml">
one: 1
two: 2
three: 3
  </pre>
</td>
</tr>

<tr>
<td>
Order changed
</td>
<td>
   <pre lang="yaml">
one: 1
two: 2
   </pre>
</td>
<td>
  <pre lang="yaml">
three: 3
two: 3
one: 3
  </pre>
</td>
<td>
  <pre lang="yaml">
three: 3
two: 2
one: 1
  </pre>
</td>
</tr>

<tr>
<td>
Padding changed
</td>
<td>
   <pre lang="yaml">
one: 
  two: 2
   </pre>
</td>
<td>
  <pre lang="yaml">
one: 
    two: 3
  </pre>
</td>
<td>
  <pre lang="yaml">
one: 
    two: 2
  </pre>
</td>
</tr>

<tr>
<td>
Comment updated
</td>
<td>
   <pre lang="yaml">
one: 
  # Old comment
  two: 2
   </pre>
</td>
<td>
  <pre lang="yaml">
one: 
    # New comment
    two: 3
  </pre>
</td>
<td>
  <pre lang="yaml">
one: 
    # New comment
    two: 2
  </pre>
</td>
</tr>

<tr>
<td>
List not merged, but padding updated
</td>
<td>
   <pre lang="yaml">
list: 
  - one
  - two
   </pre>
</td>
<td>
  <pre lang="yaml">
list: 
    - one
    - three
  </pre>
</td>
<td>
  <pre lang="yaml">
list: 
    - one
    - two
  </pre>
</td>
</tr>

<tr>
<td>
Object list item updated
</td>
<td>
   <pre lang="yaml">
list: 
  - one: 1
    two: 2
   </pre>
</td>
<td>
  <pre lang="yaml">
list: 
  - one: 1
    two: 2
    three: 3
  </pre>
</td>
<td>
  <pre lang="yaml">
list: 
  - one: 1
    two: 2
    three: 3
  </pre>
</td>
</tr>

<tr>
<td>
List declaration style could change
</td>
<td>
   <pre lang="yaml">
list: 
  - one: 1
    two: 2
   </pre>
</td>
<td>
  <pre lang="yaml">
list: 
  - 
    one: 1
    two: 2
  </pre>
</td>
<td>
  <pre lang="yaml">
list: 
  - 
    one: 1
    two: 2
  </pre>
</td>
</tr>
</table>

### Lists matching logic

Object list items updated because in some cases lists could be quite complex and contain 
updatable configuration blocks.

Items match is searched by "the most similarity": selects item containing
more properties with the same value.

* Item containing property with different value would never match
* If item contains sub-lists then at least one list item in current list must match
    - Scalar list values ignored (only object lists counted)
* Only exact match counts: if multiple items match with the same amount of matched properties
  then no item would be updated (avoid guessing, work only on exact matches)

<table>
<tr>
<th>Case</th>
<th>List item</th>
<th>Candidates</th>
<th>Match</th>
</tr>
<tr>
<td>
Match by value
</td>
<td>
   <pre lang="yaml">
one: 1
two: 2
   </pre>
</td>
<td>
  <pre lang="yaml">
- one: 1
  two: 3
- one: 1
  two: 2
  </pre>
</td>
<td>
 Item 2
</td>
</tr>

<tr>
<td>
Match by "weight"
</td>
<td>
   <pre lang="yaml">
one: 1
two: 2
   </pre>
</td>
<td>
  <pre lang="yaml">
- one: 1
  three: 3
- one: 1
  two: 2
  three: 3
  </pre>
</td>
<td>
 Item 2 (technically both matches, but first item with one and second item with 2 props)
</td>
</tr>

<tr>
<td>
Multiple matches
</td>
<td>
   <pre lang="yaml">
one: 1
   </pre>
</td>
<td>
  <pre lang="yaml">
- one: 1
  two: 3
- one: 1
  two: 2
  </pre>
</td>
<td>
 No
</td>
</tr>

<tr>
<td>
Scalar list ignored
</td>
<td>
   <pre lang="yaml">
one: 1
sub:
    - a
    - b
   </pre>
</td>
<td>
  <pre lang="yaml">
- one: 1
  sub:
    - c
    - d
- one: 1
  sub:
    - e
    - f
  </pre>
</td>
<td>
 No (both matches, scalar list values ignored)
</td>
</tr>

<tr>
<td>
Sub list (one sub list element matched)
</td>
<td>
   <pre lang="yaml">
one: 1
sub:
    - a: 1
   </pre>
</td>
<td>
  <pre lang="yaml">
- one: 1
  sub:
    - a: 2
    - a: 1
- one: 1
  sub:
    - b: 1
    - b: 2
  </pre>
</td>
<td>
 Item 1 (sub list has (at least) one match) 
</td>
</tr>
</table>

### Variables

Update config could contain variables with syntax: `#{name}`.
Such syntax used instead of more common `${..}` because:

- Config may rely on environment variables and so will collide with replacing variables
- Such syntax recognized as comment and so not processed value would be empty value after parsing.

```yaml
obj:
  some: #{name}
```

IMPORTANT: only known placeholders would be replaced (unknown variables will remain unprocessed)

Feature supposed to be used to "adopt" config for current environment before processing.
Cli modules would automatically use environment variables.

### General workflow

* Read update config
  - replace variables
  - parse with snakeyaml (validation)
  - parse with comments parser
  - compare trees (self-validation)
* Read current config
  - if not exists, simply copy update file
  - parse with snakeyaml and comments parsers and compare trees
* Perform merge
* Write to tmp file
* Read merged result with snakeyaml (validate file correctness)
* Check all new properties added and current values preserved (validation)
* Backup current config (in the same folder)
* Replace config

Two last steps performed only if file content changes, otherwise current config remain untouched (to avoid producing redundant backups).

### Comments parser specifics
 
Parser assume everything above property as property comment (even empty lines):

```yaml
# Large comment

# With empty lines
prop: 1
```

Exactly because of this comment is always replaced from new file.

For example, assume reordering case:

Original config:

```yaml
# Large header

# property comment
prop1: 1
prop2: 2
```

New config:

```yaml
# Large header

prop2: 2
# property comment
prop1: 1
```

Without using comments from new file, entire header would be moved below the first property,
but with comments update header would remain untouched.

But still, some edge cases possible (with removed properties).

#### Values

Parser stores property values as everything after colon (including possible in-line comments).
The same for multi-line values. 

This way, current value could be restored *exactly* the same as it was in the original file
(to avoid re-formatting).

And because of this in-line comments are not recognized and so could "survive" update.

#### Multi-line values

It might be not obvious, but in most cases multi-line value includes empty line
after last value line. In some situation this might lead to confusion:

Source config:

```yaml
some: 1

prop: multi-line
  value
```

Update config:

```yaml
some: 1

other: 1
```

Merge result would be:

```yaml
some: 1

prop: multi-line
  value


other: 1
```

Note double empty lines at the end: first line appeared as part of multi-line value
and other line is `other` property "comment" (empty line preserved as comments to preserve file structure).

Such cases may confuse, but it's correct behaviour.

---
[![java lib generator](http://img.shields.io/badge/Powered%20by-%20Java%20lib%20generator-green.svg?style=flat-square)](https://github.com/xvik/generator-lib-java)