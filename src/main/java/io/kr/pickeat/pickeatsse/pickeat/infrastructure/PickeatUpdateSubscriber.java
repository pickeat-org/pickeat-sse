package io.kr.pickeat.pickeatsse.pickeat.infrastructure;

import io.kr.pickeat.pickeatsse.pickeat.application.PickeatUpdateBuffer;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * Redis 채널로 들어온 갱신 신호를 이 인스턴스가 보유한 emitter 들에게 전달한다.
 * 모든 SSE 서버 인스턴스가 같은 채널을 구독하므로, 참가자가 어느 인스턴스에 붙어 있든 알림을 받는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PickeatUpdateSubscriber implements MessageListener {

    private final PickeatUpdateBuffer pickeatUpdateBuffer;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String pickeatCode = new String(message.getBody(), StandardCharsets.UTF_8);
        log.debug("픽잇 갱신 신호 수신 - pickeatCode={}", pickeatCode);
        pickeatUpdateBuffer.mark(pickeatCode);
    }
}
