.DEFAULT_GOAL := _build/clojure_build.md

.PHONY: typos typos-fix clean help

_build/%.md: prompts/%.md
	pandoc \
	--metadata-file prompts/$*.md \
	-f markdown -t markdown \
	$(shell echo prompts/$*.md && yq '.sections[]' prompts/$*.md) \
	-o $@
	@echo "build successful: $@"

# Check for typos in the codebase
typos:
	@echo "Checking for typos..."
	typos

# Fix typos automatically
typos-fix:
	@echo "Fixing typos..."
	typos --write-changes

# Show available targets
help:
	@echo "Available targets:"
	@echo "  make                  - Build _build/clojure_build.md (default)"
	@echo "  make typos            - Check for typos"
	@echo "  make typos-fix        - Fix typos automatically"
	@echo "  make _build/%.md      - Build specific markdown file"
	@echo "  make help             - Show this help message"

clean:
	rm -rf _build/*.md
