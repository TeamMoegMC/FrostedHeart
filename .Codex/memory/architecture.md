# Architecture & Conventions

## Key Architecture Patterns
1. **Topic-based packaging**: Content organized by game system (town, climate, energy), not by code type
2. **Codec-first serialization**: Mojang's Codec system used extensively for data
3. **Forge Capabilities**: Custom capability wrappers with NBT/Codec/Transient variants
4. **IE Multiblock integration**: Immersive Engineering multiblock base classes
5. **Create integration**: Kinetic block support
6. **CUI Framework**: Custom UI system with themes, widgets, screen adapters
7. **Team data system**: Player teams with FTB Teams fallback to single-player

## Build & Run
```bash
./gradlew build          # Build
./gradlew runClient      # Run client
./gradlew runServer      # Run server
```

## Documentation Status
- chorda/: 377 files with bilingual (CN/EN) Javadoc + 58 package-info.java
- frostedheart/: Not yet documented
- frostedresearch/: Not yet documented

## Comment Format Convention
```java
/**
 * 中文类/方法描述。
 * <p>
 * English class/method description.
 *
 * @param name 中文参数描述 / English parameter description
 * @return 中文返回值描述 / English return value description
 */
```
