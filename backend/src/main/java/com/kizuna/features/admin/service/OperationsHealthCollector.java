package com.kizuna.features.admin.service;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Configuration @EnableScheduling
public class OperationsHealthCollector {
    private final JdbcTemplate jdbc;
    private final OperationsActorResolver actors;
    private final Clock clock;
    private final ExecutorService workers=Executors.newFixedThreadPool(2,r->{ var t=new Thread(r,"operations-probe"); t.setDaemon(true); return t; });
    private final AtomicBoolean collecting=new AtomicBoolean();
    private volatile List<Observation> snapshot=List.of();

    public OperationsHealthCollector(@Qualifier("operationsIdentityJdbc") JdbcTemplate jdbc,OperationsActorResolver actors,Clock operationsClock) {
        this.jdbc=jdbc; this.actors=actors; this.clock=operationsClock;
    }
    @Scheduled(fixedDelay=30000,initialDelay=0)
    public void collect() {
        if(!collecting.compareAndSet(false,true)) return;
        try {
            long start=System.nanoTime();
            var future=workers.submit(()->database());
            var observations=new ArrayList<Observation>();
            observations.add(new Observation("APPLICATION","Hoạt động",Instant.now(clock),null,"Tiến trình đang phục vụ; chưa đo khả dụng từ bên ngoài."));
            try { observations.add(future.get(2,TimeUnit.SECONDS)); }
            catch(Exception ex) { future.cancel(true); observations.add(new Observation("DATABASE","Gián đoạn",Instant.now(clock),null,"Không nhận được kết quả kiểm tra trong thời gian cho phép.")); }
            observations.add(new Observation("AGENT_MEMORY","Chưa xác định",null,null,"Chưa có nguồn kiểm tra được xác minh."));
            if(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime()-start)<4) snapshot=List.copyOf(observations);
        } finally { collecting.set(false); }
    }
    private Observation database() {
        long start=System.nanoTime();
        try {
            Integer result=jdbc.queryForObject("SELECT 1",Integer.class);
            return new Observation("DATABASE",Integer.valueOf(1).equals(result)?"Hoạt động":"Chưa xác định",Instant.now(clock),TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start),"Kiểm tra kết nối chỉ đọc.");
        } catch(Exception ex) { return new Observation("DATABASE","Gián đoạn",Instant.now(clock),null,"Chưa kết nối được cơ sở dữ liệu."); }
    }
    public Map<String,Object> read() { actors.requireAdmin(); return currentSnapshot(); }
    public Map<String,Object> currentSnapshot() {
        var now=Instant.now(clock);
        var components=snapshot.stream().map(o-> {
            var m=new LinkedHashMap<String,Object>(); m.put("componentCode",o.component()); m.put("label",OperationsComponentCatalog.LABELS.get(o.component()));
            m.put("statusLabel",o.status()); m.put("observedAt",o.observedAt()); m.put("responseTimeMs",o.responseTimeMs());
            m.put("stale",o.observedAt()!=null && Duration.between(o.observedAt(),now).compareTo(Duration.ofSeconds(60))>0);
            m.put("reasonLabel",o.reason()); m.put("scopeLabel",OperationsComponentCatalog.LABELS.get(o.component())); return m;
        }).toList();
        return Map.of("generatedAt",now,"components",components);
    }
    @PreDestroy public void close() { workers.shutdownNow(); }
    public record Observation(String component,String status,Instant observedAt,Long responseTimeMs,String reason) { }
}
