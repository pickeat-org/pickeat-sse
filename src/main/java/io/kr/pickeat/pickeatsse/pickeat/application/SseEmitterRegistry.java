package io.kr.pickeat.pickeatsse.pickeat.application;

import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
public class SseEmitterRegistry {

    private static final long EMITTER_TIMEOUT = 30 * 60 * 1000L;
    private static final String CONNECTED_EVENT = "CONNECTED";
    private static final String PICKEAT_UPDATED_EVENT = "PICKEAT_UPDATED";

    private final Map<String, List<SseEmitter>> sseEmitterMap = new ConcurrentHashMap<>();

    /**
     * 팬아웃 전송 전용 실행기.
     * 느린 클라이언트 한 명의 블로킹 write 가 같은 방의 나머지 참가자 전송을 막지 않도록
     * emitter 단위로 가상 스레드를 할당한다.
     */
    private final ExecutorService fanoutExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public SseEmitter register(String pickeatCode) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT);
        sseEmitterMap.computeIfAbsent(pickeatCode, code -> new CopyOnWriteArrayList<>())
                .add(emitter);

        Runnable cleanup = () -> remove(pickeatCode, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        try {
            emitter.send(SseEmitter.event()
                    .name(CONNECTED_EVENT)
                    .data(""));
        } catch (Exception e) {
            log.warn("SSE 최초 연결 이벤트 전송 실패 - pickeatCode={}", pickeatCode, e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    public void broadcast(String pickeatCode) {
        List<SseEmitter> sseEmitters = sseEmitterMap.get(pickeatCode);
        if (sseEmitters == null) {
            return;
        }

        for (SseEmitter emitter : sseEmitters) {
            fanoutExecutor.execute(() -> sendUpdated(pickeatCode, emitter));
        }
    }

    public boolean hasEmitters(String pickeatCode) {
        return sseEmitterMap.containsKey(pickeatCode);
    }

    private void sendUpdated(String pickeatCode, SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event()
                    .name(PICKEAT_UPDATED_EVENT)
                    .data(""));
        } catch (Exception e) {
            log.debug("SSE 전송 실패로 연결 종료 - pickeatCode={}", pickeatCode, e);
            emitter.completeWithError(e);
        }
    }

    private void remove(String pickeatCode, SseEmitter emitter) {
        sseEmitterMap.computeIfPresent(pickeatCode, (code, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }

    @PreDestroy
    void shutdown() {
        fanoutExecutor.shutdown();
    }
}
