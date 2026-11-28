# Mechanical calculator automation rubbing

- Author: Codex
- Date: 2026-11-22
- Changed calculator authorization to allow FakePlayer balance queries and bounded extraction before team resolution, without claiming or changing ownership. Tool code is unchanged.
- Updated ResearchGameTests for ownerless and owned automation, requested limits, balance caps and ownership preservation; retained real cross-team denial checks.
- Updated gameplay-and-integrations.md and known-risks.md to describe the new automation exception.
- Validation: runtime GameTests and catalogue validation have not been run.

## Follow-up correction

- Restricted the exception to DeployerFakePlayer rubbing queries/extraction with normal team validation. Ordinary fake players and all fake-player clicks remain denied; ownerless machines still require a real-player claim.
- Added actual deployer tests for same-team and cross-team access, bounds, ownership and ownerless behavior.
- Previous validation compiled production and test sources, but was interrupted while spawn preparation remained at 10%; catalogue validation was not reached. Follow-up validation pending.
