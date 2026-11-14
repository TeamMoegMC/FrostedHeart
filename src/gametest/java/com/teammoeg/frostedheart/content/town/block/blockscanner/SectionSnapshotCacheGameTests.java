package com.teammoeg.frostedheart.content.town.block.blockscanner;

import java.util.*;
import java.util.concurrent.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_section_cache")
@PrefixGameTestTemplate(false)
public final class SectionSnapshotCacheGameTests {
    private static byte[] bytes(int value){byte[] data=new byte[4096];Arrays.fill(data,(byte)value);return data;}
    private static SectionSnapshotCache.Snapshot seed(SectionSnapshotCache cache,long key){var f=cache.request(key);cache.service(0,2,k->bytes((int)k));return f.join();}
    @GameTest(template="phase0a_empty")
    public static void freshHitsDoNotQueueOrExtendTtl(GameTestHelper h){
        var c=new SectionSnapshotCache();var s=seed(c,1);c.advance(99);
        h.assertTrue(c.request(1).join()==s&&s.expiresAt()==100&&c.statistics().pending()==0,"fresh hit returns same immutable snapshot without refreshing or extending expiry");h.succeed();
    }
    @GameTest(template="phase0a_empty")
    public static void staleHitReturnsImmediatelyAndRefreshesOnce(GameTestHelper h){
        var c=new SectionSnapshotCache();var old=seed(c,1);c.advance(100);
        for(int i=0;i<50;i++)h.assertTrue(c.request(1).isDone()&&c.request(1).join()==old,"stale readers must not wait");
        h.assertTrue(c.statistics().pending()==1&&old.removeAt()==140,"one queued refresh, fixed grace deadline");
        c.service(101,2,k->bytes(9));var next=c.request(1).join();
        h.assertTrue(next!=old&&next.valueAt(0)==9&&old.valueAt(0)==1&&old.retired()&&next.expiresAt()==201,"publish a new snapshot without mutating old bytes");h.succeed();
    }
    @GameTest(template="phase0a_empty")
    public static void graceEndRemovesAndRevertsToColdMiss(GameTestHelper h){
        var c=new SectionSnapshotCache();var old=seed(c,1);c.advance(139);h.assertTrue(c.request(1).join()==old,"last grace tick readable");
        c.advance(140);h.assertTrue(c.statistics().entries()==0&&c.statistics().pending()==0&&old.retired(),"hard expiry removes bytes and obsolete stale-only work");
        var fresh=c.request(1);h.assertTrue(!fresh.isDone(),"after grace a reader must wait for new bytes");
        c.service(140,1,k->bytes(7));h.assertTrue(fresh.join().valueAt(0)==7,"cold refresh completes");h.succeed();
    }
    @GameTest(template="phase0a_empty")
    public static void unaccessedExpiredEntriesDoNotRefresh(GameTestHelper h){
        var c=new SectionSnapshotCache();seed(c,1);c.service(100,2,k->{throw new AssertionError("expiry alone must not enqueue work");});
        h.assertTrue(c.statistics().entries()==1&&c.statistics().pending()==0,"unread stale entry remains only for grace");
        c.advance(140);h.assertTrue(c.statistics().entries()==0,"unread stale entry is removed on schedule");h.succeed();
    }
    @GameTest(template="phase0a_empty")
    public static void dirtyTtlShortensWithoutSliding(GameTestHelper h){
        var c=new SectionSnapshotCache();var s=seed(c,1);c.markDirty(1,10);h.assertTrue(s.expiresAt()==30,"dirty TTL is twenty ticks from first write");
        c.markDirty(1,20);c.markDirty(1,29);h.assertTrue(s.expiresAt()==30,"continuous writes cannot keep pushing expiry back");
        c.advance(30);c.request(1);c.markDirty(1,35);h.assertTrue(s.removeAt()==70,"dirty during grace does not restart grace");
        c.advance(70);h.assertTrue(c.statistics().entries()==0,"dirty hard deadline honored");h.succeed();
    }
    @GameTest(template="phase0a_empty")
    public static void refreshQueueIsFifoAndBudgeted(GameTestHelper h){
        var c=new SectionSnapshotCache();seed(c,1);c.advance(100);
        var cold=c.request(2);c.request(1);for(int i=0;i<20;i++)c.request(1);c.request(3);
        List<Long> order=new ArrayList<>();c.service(100,2,k->{order.add(k);return bytes((int)k);});
        h.assertTrue(order.equals(List.of(2L,1L))&&cold.isDone()&&c.statistics().pending()==1,"stale work is appended once; never jumps ahead of existing work; two-load budget");h.succeed();
    }
    @GameTest(template="phase0a_empty")
    public static void concurrentColdReadersShareOneRequest(GameTestHelper h){
        var c=new SectionSnapshotCache();List<CompletableFuture<SectionSnapshotCache.Snapshot>> readers=new ArrayList<>();
        for(int i=0;i<20;i++)readers.add(CompletableFuture.supplyAsync(()->c.request(42)).join());
        h.assertTrue(readers.stream().allMatch(f->f==readers.get(0))&&c.statistics().pending()==1,"deduplicate worker requests for the same section");
        c.service(0,2,k->bytes(3));h.assertTrue(readers.stream().allMatch(f->f.join()==readers.get(0).join())&&c.statistics().loads()==1,"one copy serves every waiter");h.succeed();
    }
    @GameTest(template="phase0a_empty")
    public static void lruCapacityRetiresSnapshots(GameTestHelper h){
        var c=new SectionSnapshotCache(100,20,40,2);seed(c,1);var second=seed(c,2);c.request(1);
        var third=c.request(3);c.service(0,2,k->bytes(3));h.assertTrue(third.isDone()&&c.statistics().entries()==2&&second.retired(),"bounded cache evicts least recently accessed snapshot");
        h.assertTrue(!c.request(2).isDone(),"evicted data cannot be returned as a cache hit");h.succeed();
    }
    @GameTest(template="phase0a_empty")
    public static void unloadedMasksAreNotCached(GameTestHelper h){
        var c=new SectionSnapshotCache();var missing=c.request(1);c.service(0,2,k->null);
        h.assertTrue(missing.join().missing()&&missing.join().valueAt(0)==AbstractWorld.BlockType.WALL.ordinal()&&c.statistics().entries()==0,"unloaded mask must not poison the cache");
        var loaded=c.request(1);h.assertTrue(!loaded.isDone(),"a later loaded chunk is immediately eligible to load");c.service(1,2,k->bytes(9));h.assertTrue(loaded.join().valueAt(0)==9,"new terrain replaces mask");h.succeed();
    }
    @GameTest(template="phase0a_empty")
    public static void invalidationDuringLoadCannotRepublish(GameTestHelper h){
        var c=new SectionSnapshotCache();var future=c.request(1);c.service(0,2,k->{c.invalidateAll();return bytes(1);});
        h.assertTrue(future.isCompletedExceptionally()&&c.statistics().entries()==0,"late loader result cannot resurrect invalidated data");
        c.close();h.assertTrue(c.request(1).isCompletedExceptionally(),"shutdown releases blocked callers");h.succeed();
    }
    @GameTest(template="phase0a_empty")
    public static void clockRollbackAndLoaderFailureRecover(GameTestHelper h){
        var c=new SectionSnapshotCache();seed(c,1);c.advance(90);c.advance(20);h.assertTrue(c.statistics().entries()==0,"clock rollback clears stale lifetimes");
        var failed=c.request(2);c.service(20,1,k->{throw new IllegalStateException("test loader failure");});
        h.assertTrue(failed.isCompletedExceptionally()&&c.statistics().pending()==0,"failed load releases the request slot");
        var retry=c.request(2);c.service(21,1,k->bytes(2));h.assertTrue(retry.join().valueAt(0)==2,"later requests retry normally");h.succeed();
    }
    @GameTest(template="phase0a_empty",timeoutTicks=200)
    public static void actualBlockWritesShortenWorldCacheTtl(GameTestHelper h){
        var level=h.getLevel();BlockPos p=h.absolutePos(new BlockPos(5,3,5));p=new BlockPos(p.getX(),96,p.getZ());
        level.setBlock(p,Blocks.STONE.defaultBlockState(),2);final BlockPos pos=p;
        var cache=TownSectionSnapshots.get(level);var pending=cache.request(SectionPos.asLong(pos));
        h.runAtTickTime(5,()->{
            h.assertTrue(pending.isDone(),"server END services shared cache without a town worker");var old=pending.join();
            int index=((pos.getY()&15)<<8)|((pos.getZ()&15)<<4)|(pos.getX()&15);
            h.assertTrue(old.valueAt(index)==AbstractWorld.BlockType.WALL.ordinal(),"real world stone classified");
            long before=old.expiresAt(),now=level.getGameTime();level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
            h.assertTrue(old.expiresAt()<=now+20&&old.expiresAt()<before,"actual state-write mixin applies dirty TTL");
            h.runAtTickTime(30,()->{
                var stale=cache.request(SectionPos.asLong(pos));h.assertTrue(stale.isDone()&&stale.join().valueAt(index)==AbstractWorld.BlockType.WALL.ordinal(),"dirty grace returns old bytes immediately");
                h.runAtTickTime(33,()->{h.assertTrue(cache.request(SectionPos.asLong(pos)).join().valueAt(index)==AbstractWorld.BlockType.AIR.ordinal(),"budgeted refresh sees actual air");h.succeed();});
            });
        });
    }
    @GameTest(template="phase0a_empty",timeoutTicks=200)
    public static void sectionBoundaryDirtyMarksBothSides(GameTestHelper h){
        var level=h.getLevel();var origin=h.absolutePos(new BlockPos(5,3,5));int boundary=(origin.getX()>>4)*16+15;
        BlockPos p=new BlockPos(boundary,160,origin.getZ());var cache=TownSectionSnapshots.get(level);
        var left=cache.request(SectionPos.asLong(p));var right=cache.request(SectionPos.asLong(p.east()));
        h.runAtTickTime(5,()->{
            h.assertTrue(left.isDone()&&right.isDone(),"two sections share the same per-level budget");
            level.setBlock(p,Blocks.STONE.defaultBlockState(),2);long limit=level.getGameTime()+20;
            h.assertTrue(left.join().expiresAt()<=limit&&right.join().expiresAt()<=limit,"section edge invalidates neighbouring collision context too");h.succeed();
        });
    }
}
