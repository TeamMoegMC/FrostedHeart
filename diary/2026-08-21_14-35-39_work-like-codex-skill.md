# Work-like-Codex shared agent skill

- Time: `2026-08-21 14:35:39 +08:00`
- Author: `Codex; OpenAI GPT-5; skill development agent`
- Status: `completed`
- Scope: `.agents/skills/work-like-codex/; diary/`

## Completed

- Added a project-shared Agent Skill that gives non-Codex coding agents a Codex-compatible startup and engineering workflow without misrepresenting their model or runtime identity.
- Added read-only, cross-platform Node discovery for applicable `AGENTS.md` files, Markdown Codex memory, project `.Codex/memory`, imported OpenClaw Codex memory, and shared skill roots.
- Added a focused inspect-plan-edit-validate workflow covering scoped instructions, dirty-worktree preservation, documentation, project history, safety, and completion reporting.
- Added a self-contained discovery test that uses an isolated temporary Git repository and verifies instruction order, memory order, skill roots, and credential-file exclusion.

## Decisions

- Placed the skill under `.agents/skills` because both OpenClaw and DeepSeek Harness can use that project-local AgentSkills root while Codex-compatible runtimes can consume the same checked-in workflow.
- Kept context discovery read-only and path-only. The script never parses credentials, SQLite memory, raw sessions, or file contents; the agent reads only the reported Markdown context under explicit skill rules.
- Reproduced Codex's working contract rather than claiming model or harness identity. Native sandbox, approval, and host instruction boundaries remain authoritative.
- Kept project-specific knowledge in `AGENTS.md` and `.Codex/memory` rather than duplicating Frosted Heart rules in the reusable skill. No living `docs/` update was needed because gameplay behavior and cross-system contracts did not change.

## Validation

- `node --check` passed for `scripts/discover-context.mjs` and `scripts/discover-context.test.mjs`.
- `node scripts/discover-context.test.mjs` passed against its isolated temporary repository.
- Running discovery in Frosted Heart found the root `AGENTS.md`, both project-memory files, and the project, Codex, and personal AgentSkills roots; JSON output parsed successfully.
- Frontmatter-equivalent validation passed, no template placeholders remained, and no trailing whitespace was found.
- The skill-creator `quick_validate.py` entry point was attempted but its environment lacked `PyYAML`; temporary dependency installation was blocked by host policy. OpenClaw and DSH native discovery tests were not run because neither executable is installed on this machine.
- No Gradle build was run because mod source, resources, data, and gameplay documentation were unchanged.

## Remaining

- Run native skill discovery and one realistic read-only task in OpenClaw or DeepSeek Harness when either runtime is available.
