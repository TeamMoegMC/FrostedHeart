package com.teammoeg.frostedheart.content.town.block.blockscanner;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.LongFunction;

/** Thread-safe stale-while-revalidate cache. Only the owning main thread advances time and loads bytes. */
public final class SectionSnapshotCache {
    public static final int TTL_TICKS = 100;
    public static final int DIRTY_TTL_TICKS = 20;
    public static final int GRACE_TICKS = 40;
    public static final int MAX_SECTIONS = 1024;
    private static final byte[] MISSING = new byte[4096];
    static { Arrays.fill(MISSING, (byte) AbstractWorld.BlockType.WALL.ordinal()); }

    public static final class Snapshot {
        private final byte[] cells;
        private final int grace;
        private final boolean missing;
        private volatile long expiresAt;
        private volatile boolean retired;
        private final CompletableFuture<Snapshot> ready = CompletableFuture.completedFuture(this);
        private Snapshot(byte[] cells,long expiresAt,int grace,boolean missing) {
            this.cells=cells;this.expiresAt=expiresAt;this.grace=grace;this.missing=missing;
        }
        public byte valueAt(int index) { return cells[index]; }
        public long expiresAt() { return expiresAt; }
        public long removeAt() { return expiresAt+grace; }
        public boolean retired() { return retired; }
        public boolean missing() { return missing; }
    }
    private static final class Pending {
        final long section;
        final CompletableFuture<Snapshot> result=new CompletableFuture<>();
        boolean required;
        Pending(long section,boolean required){this.section=section;this.required=required;}
    }
    public record Statistics(int entries,int pending,long freshHits,long staleHits,long misses,long loads,long expired,long evicted) {}
    private final int ttl,dirtyTtl,grace,capacity;
    private final LinkedHashMap<Long,Snapshot> entries=new LinkedHashMap<>(64,.75f,true);
    private final LinkedHashMap<Long,Pending> queue=new LinkedHashMap<>();
    private final Map<Long,Pending> pending=new HashMap<>();
    private volatile long now;
    private boolean closed;
    private long freshHits,staleHits,misses,loads,expired,evicted;

    public SectionSnapshotCache(){this(TTL_TICKS,DIRTY_TTL_TICKS,GRACE_TICKS,MAX_SECTIONS);}
    public SectionSnapshotCache(int ttl,int dirtyTtl,int grace,int capacity) {
        this.ttl=ttl;this.dirtyTtl=dirtyTtl;this.grace=grace;this.capacity=capacity;
    }
    public long currentTick(){return now;}
    public synchronized CompletableFuture<Snapshot> request(long section) {
        if(closed)return CompletableFuture.failedFuture(new CancellationException("section cache closed"));
        Snapshot snapshot=entries.get(section);
        if(snapshot!=null&&now>=snapshot.removeAt()){remove(section,snapshot);expired++;snapshot=null;}
        if(snapshot!=null) {
            if(now<snapshot.expiresAt){freshHits++;}
            else {staleHits++;enqueue(section,false);}
            return snapshot.ready;
        }
        misses++;
        return enqueue(section,true).result;
    }
    private Pending enqueue(long key,boolean required) {
        Pending existing=pending.get(key);
        if(existing!=null){existing.required|=required;return existing;}
        Pending job=new Pending(key,required);pending.put(key,job);queue.put(key,job);return job;
    }
    /** Dirty writes shorten the deadline, never extend it or restart an existing grace period. */
    public synchronized void markDirty(long section,long gameTick) {
        Snapshot snapshot=entries.get(section);
        if(snapshot!=null)snapshot.expiresAt=Math.min(snapshot.expiresAt,gameTick+dirtyTtl);
    }
    public synchronized void markMatchingDirty(java.util.function.LongPredicate matches,long gameTick){
        entries.forEach((key,snapshot)->{if(matches.test(key))snapshot.expiresAt=Math.min(snapshot.expiresAt,gameTick+dirtyTtl);});
    }
    public synchronized void markAllDirty(long gameTick){for(Snapshot snapshot:entries.values())snapshot.expiresAt=Math.min(snapshot.expiresAt,gameTick+dirtyTtl);}
    public synchronized void advance(long gameTick) {
        if(closed)return;
        if(gameTick<now)invalidateAll();
        now=gameTick;
        Iterator<Map.Entry<Long,Snapshot>> it=entries.entrySet().iterator();
        while(it.hasNext()) {
            var entry=it.next();
            if(gameTick>=entry.getValue().removeAt()) {
                it.remove();entry.getValue().retired=true;dropStaleJob(entry.getKey());expired++;
            }
        }
    }
    /** Each successful loader result transfers ownership of an immutable byte array to this cache. Null means unloaded. */
    public void service(long gameTick,int budget,LongFunction<byte[]> loader) {
        advance(gameTick);
        for(int count=0;count<budget;count++) {
            Pending job;
            synchronized(this){
                if(closed||queue.isEmpty())return;
                var it=queue.entrySet().iterator();job=it.next().getValue();it.remove();
            }
            byte[] data;
            try {data=loader.apply(job.section);}
            catch(RuntimeException failure){
                synchronized(this){if(pending.get(job.section)==job)pending.remove(job.section);}
                job.result.completeExceptionally(failure);continue;
            }
            Snapshot published=null;
            synchronized(this){
                if(!closed&&pending.get(job.section)==job){
                    pending.remove(job.section);loads++;
                    if(data==null)published=new Snapshot(MISSING,now+1,0,true);
                    else {
                        published=new Snapshot(data,now+ttl,grace,false);
                        Snapshot old=entries.put(job.section,published);if(old!=null)old.retired=true;
                        while(entries.size()>capacity){var it=entries.entrySet().iterator();var oldest=it.next();it.remove();oldest.getValue().retired=true;dropStaleJob(oldest.getKey());evicted++;}
                    }
                }
            }
            if(published!=null)job.result.complete(published);
        }
    }
    /** Remove only actual snapshots; cold readers still receive an unloaded mask from the main-thread loader. */
    public synchronized void invalidateMatching(java.util.function.LongPredicate matches) {
        Iterator<Map.Entry<Long,Snapshot>> it=entries.entrySet().iterator();
        while(it.hasNext()){var entry=it.next();if(matches.test(entry.getKey())){it.remove();entry.getValue().retired=true;dropStaleJob(entry.getKey());}}
    }
    private void remove(long key,Snapshot snapshot){entries.remove(key);snapshot.retired=true;dropStaleJob(key);}
    private void dropStaleJob(long key){
        Pending job=pending.get(key);
        if(job!=null&&!job.required){pending.remove(key);queue.remove(key);job.result.completeExceptionally(new CancellationException("stale snapshot retired"));}
    }
    public synchronized void invalidateAll(){
        entries.values().forEach(snapshot->snapshot.retired=true);entries.clear();queue.clear();
        pending.values().forEach(job->job.result.completeExceptionally(new CancellationException("section cache invalidated")));pending.clear();
    }
    public synchronized void close(){closed=true;invalidateAll();}
    public synchronized Statistics statistics(){return new Statistics(entries.size(),pending.size(),freshHits,staleHits,misses,loads,expired,evicted);}
}
