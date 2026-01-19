package io.kr.pickeat.pickeatsse.pickeat.ui;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class PickeatController {

    private final Map<String, List<SseEmitter>> sseEmitterMap = new ConcurrentHashMap<>();

    @GetMapping("/sse/pickeat/{pickeatCode}")
    public SseEmitter connect(@PathVariable String pickeatCode) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 1시간
        sseEmitterMap.computeIfAbsent(pickeatCode, code -> new CopyOnWriteArrayList<>())
                .add(emitter);

        Runnable cleanup = () -> {
            List<SseEmitter> list = sseEmitterMap.get(pickeatCode);
            if (list != null) {
                list.remove(emitter);
                if (list.isEmpty()) {
                    sseEmitterMap.remove(pickeatCode);
                }
            }
        };

        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECTED"));
        } catch (Exception e) {
            cleanup.run();
        }

        return emitter;
    }

    @PostMapping("/internal/sse/pickeat/{pickeatCode}")
    public ResponseEntity<Void> pickeatUpdated(@PathVariable String pickeatCode) {
        List<SseEmitter> sseEmitters = sseEmitterMap.get(pickeatCode);
        if (sseEmitters == null) {
            return ResponseEntity.noContent().build();
        }

        for (var emitter : sseEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("PICKEAT_UPDATED"));
            } catch (Exception e) {
                sseEmitters.remove(emitter);
            }
        }

        return ResponseEntity.ok().build();
    }
}
