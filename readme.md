# Clojure Skills

A collect of System Prompt fragments that can make working with
Clojure in [opencode](https://opencode.ai/) more effective.

This project consist of a collection of skills and prompts that you
can mix and compose to create an effective coding agent for your code
base.

## Installation

```shell
brew bundle
```

## Skills

These skills are broken into different sub sections

- Language
  - clojure_intro.md
  - repl_workflow.md
  - immutable_data.md

- Clojure MCP
  - clojure_eval.md

- HTTP Server

    - http_kit.md


## Prompts

Skills can be combined into prompts that OpenCode is able to
use. See prompts/clojure_build.md for an example.

## Development

### Spell Checking

This project uses [typos](https://github.com/crate-ci/typos) for spell checking source code.

**Check for typos:**
```shell
make typos
# or
bb typos
```

**Automatically fix typos:**
```shell
make typos-fix
# or
bb typos-fix
```

**Configuration:**

The `_typos.toml` file contains project-specific configuration for
handling false positives and excluding directories. See the [typos
documentation](https://github.com/crate-ci/typos/blob/master/docs/reference.md)
for more details.
