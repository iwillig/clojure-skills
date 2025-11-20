---
name: clojure_namespace_naming
description: |
  Naming conventions for Clojure namespaces and namespace segments. Use when
  creating new namespaces, organizing project structure, or naming libraries.
  Use when the user mentions namespace naming, ns conventions, project organization,
  or multi-segment namespace design.
source: https://github.com/bbatsov/clojure-style-guide
---

# Namespace Naming

Namespace naming follows conventions that prevent conflicts and communicate structure.

## Key Conventions

**Multi-Segment Namespaces**: Avoid single-segment namespaces. Use at least two segments to prevent conflicts.

```clojure
;; Good
(ns example.core)
(ns company.project.api)

;; Bad - single segment
(ns example)
(ns mylib)
```

**Common Naming Schemas**: Follow established patterns for namespace organization.

```clojure
;; Pattern 1: project.module
(ns myapp.database)
(ns myapp.handlers)

;; Pattern 2: organization.project.module
(ns acme.widget-maker.core)
(ns acme.widget-maker.api)

;; Pattern 3: domain.library-name
(ns io.github.username.mylib)
```

**Segment Limits**: Avoid overly long namespaces (more than 5 segments). Deep nesting creates friction.

```clojure
;; Good - reasonable depth
(ns company.project.api.handlers)

;; Bad - too deep
(ns com.company.division.team.project.module.submodule.handlers)
```

**Composite Segments**: Use `lisp-case` (kebab-case) for multi-word segments.

```clojure
;; Good
(ns bruce.project-euler)
(ns company.order-processing)

;; Bad - other cases
(ns bruce.projectEuler)
(ns company.order_processing)
```

**Core Namespace**: Use `project.core` only for projects with a single implementation namespace. For multi-namespace projects, use more specific names.

```clojure
;; Good - single namespace project
(ns calculator.core)

;; Good - multiple namespaces
(ns web-app.database)
(ns web-app.handlers)
(ns web-app.middleware)

;; Questionable - "core" in multi-ns project
(ns web-app.core)  ; What does this contain?
```

**Library Coordinates**: For public libraries, align namespace with Maven coordinates. Use domain name or project name as groupId.

```clojure
;; Following domain ownership
com.stuartsierra/component
;; Namespace: com.stuartsierra.component

;; Following project name
cider/cider-nrepl
;; Namespace: cider.nrepl
```

## Why This Matters

Namespace naming prevents conflicts in the global namespace. Multi-segment names ensure your code won't clash with other libraries. Consistent naming patterns help developers navigate unfamiliar codebases. These conventions balance uniqueness with usability.

## Resources

- [Clojure Style Guide](https://github.com/bbatsov/clojure-style-guide) - Original style guide by Bozhidar Batsov
- [Clojure Community Style Guide](https://guide.clojure.style) - Web version
