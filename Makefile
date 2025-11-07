.DEFAULT_GOAL := _build/clojure_build.md

.PHONY: typos typos-fix clean help

# Define variables
BUILD_DIR := _build
PROMPT_DIR := prompts
METADATA_TEMPLATE := prompt_templates/metadata.plain

# Function to extract sections from YAML frontmatter
# Usage: $(call extract-sections,input-file)
extract-sections = $(shell pandoc $(1) --template=$(METADATA_TEMPLATE) | jq -r '.sections[]? // empty')

# Function to build a markdown file with all its sections
# Usage: $(call build-with-sections,input-file,output-file)
define build-with-sections
	@echo "Building $(2)..."
	@mkdir -p $(dir $(2))
	@pandoc $(1) -f markdown -t markdown -o $(2)
	@for section in $(call extract-sections,$(1)); do \
		if [ -f "$$section" ]; then \
			echo "" >> $(2); \
			echo "# Including: $$section" >> $(2); \
			echo "" >> $(2); \
			pandoc "$$section" -f markdown -t markdown -o - >> $(2); \
		else \
			echo "Warning: Section file not found: $$section"; \
		fi; \
	done
	@echo "Build successful: $(2)"
endef

# Pattern rule to build markdown files from prompts
$(BUILD_DIR)/%.md: $(PROMPT_DIR)/%.md
	$(call build-with-sections,$<,$@)

# Check for typos in the codebase
typos:
	@echo "Checking for typos..."
	@typos

# Fix typos automatically
typos-fix:
	@echo "Fixing typos..."
	@typos --write-changes

# Show available targets
help:
	@echo "Available targets:"
	@echo "  make                  - Build $(BUILD_DIR)/clojure_build.md (default)"
	@echo "  make typos            - Check for typos"
	@echo "  make typos-fix        - Fix typos automatically"
	@echo "  make $(BUILD_DIR)/%.md      - Build specific markdown file"
	@echo "  make help             - Show this help message"
	@echo "  make clean            - Remove built files"

# Clean build artifacts
clean:
	@echo "Cleaning $(BUILD_DIR)..."
	@rm -rf $(BUILD_DIR)/*.md
