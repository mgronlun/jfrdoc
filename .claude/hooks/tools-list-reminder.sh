#!/usr/bin/env bash
# PostToolUse hook: when a file that defines tool surface is touched
# (jfrdoc.java for the TOOLS registry, or any Jfr*Tool.java for the
# implementation), remind the model of the cross-cutting docs/prompts/
# fixtures the compiler cannot enforce. The reminder is harmless for
# internal-only refactors — the model self-filters.

set -u

file=$(jq -r '.tool_input.file_path // empty')
case "$(basename -- "$file" 2>/dev/null)" in
  jfrdoc.java|Jfr*Tool.java) ;;
  *) exit 0 ;;
esac

# read -d '' is used (not msg=$(cat <<EOF...)) because the latter mis-parses
# when the heredoc body contains an apostrophe inside a command substitution.
read -r -d '' msg <<'EOF' || true
You touched a tool's surface. The compiler catches a missing class or
wiring, but the docs, prompts, and fixtures that mention each tool by
name (or by JSON field path) have no compile-time check.

Discovery strategy — treat existing tools as the spec:
  - For add/delete: pick a tool currently in TOOLS and grep the repo
    for its name in every form it takes (code identifier, CLI/file
    spelling, fixture filename — look at how an existing tool appears
    to learn the forms). Every place that lists, counts, gates,
    links, or invokes it is a place the new tool must appear (or be
    removed from). Committed JSON fixtures count too — add or delete
    a matching file alongside.
  - For an update: if you changed the tool's output JSON keys, input
    schema, name, or description, grep the repo for the old string
    and either update or remove every hit. If output schema changed,
    the committed fixture for that tool needs regeneration, not
    editing. Internal-only refactors (helpers, performance, comments)
    need no further action — ignore this reminder.

If grepping an existing tool name returns zero hits across the repo,
the convention has changed — stop and ask, do not invent a place to
edit.
EOF

jq -n --arg msg "$msg" '{hookSpecificOutput:{hookEventName:"PostToolUse",additionalContext:$msg}}'
