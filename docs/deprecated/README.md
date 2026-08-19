# Deprecated Documentation

This directory preserves obsolete documents whose historical models, decisions, or reasoning may still be useful. Its contents are not living documentation and must not be used as evidence of current game behavior.

## Rules

1. Keep deprecated documents out of system READMEs' primary reading paths. A system README may link them in a clearly labeled historical section.
2. Add a visible deprecation notice stating why the document is obsolete and where readers should look for current information.
3. Do not keep updating a deprecated document to match current implementation. Correct only dangerous factual ambiguity in the deprecation notice itself.
4. Move a document here only after identifying its replacement or clearly stating that no replacement exists.
5. Do not use this directory for open proposals, unfinished implementation plans, or conversations. Those belong in `plans/` or `discussion/`.
6. Do not move material here merely to avoid deciding whether it remains useful. Git and the development diary already preserve history that has no ongoing reference value.

## Contents

| Document | Deprecated because | Current reference |
|---|---|---|
| [TWR town numerical model design](TWR%E5%9F%8E%E9%95%87%E6%95%B0%E5%80%BC%E6%A8%A1%E5%9E%8B%E8%AE%BE%E8%AE%A1.md) | Its compatibility model, target thermal model, calibration, and script workflow were superseded by later Java town-model and gameplay implementation work. | [Town documentation](../town/README.md) and [current town model](../town/town-model.md) |
