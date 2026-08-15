package io.kr.pickeat.pickeatsse.pickeat.ui;

import io.kr.pickeat.pickeatsse.pickeat.application.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
public class PickeatController {

    private final SseEmitterRegistry sseEmitterRegistry;

    @GetMapping("/sse/pickeat/{pickeatCode}")
    public SseEmitter connect(@PathVariable String pickeatCode) {
        return sseEmitterRegistry.register(pickeatCode);
    }
}
