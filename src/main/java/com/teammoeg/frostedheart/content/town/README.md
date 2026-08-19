# Town Package

This package implements town state, buildings, residents, resources, daily settlement, citizen presence, networking, and management UI. Java source and registered data are authoritative.

Start with [`TeamTownData`](TeamTownData.java) for persistent state and lifecycle, [`TeamTown`](TeamTown.java) for mutations, and [`ITownBuilding`](building/ITownBuilding.java) for building serialization.

Read the [town documentation](../../../../../../../../docs/town/README.md) for system behavior and the [implementation reference](../../../../../../../../docs/town/implementation-reference.md) before extending this package. Update the owning documentation, tests, and diary entry with behavioral changes.
