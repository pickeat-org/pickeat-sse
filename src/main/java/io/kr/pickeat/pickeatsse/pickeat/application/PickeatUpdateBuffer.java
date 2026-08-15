package io.kr.pickeat.pickeatsse.pickeat.application;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 방마다 슬롯 한 칸만 두고 갱신 신호를 합친다(conflation).
 * <p>
 * 신호에 데이터가 실려 있지 않으므로(zero-payload) 여러 번의 신호를 한 번으로 합쳐도 정보 손실이 없다.
 * 클라이언트가 하는 일은 어느 쪽이든 "재조회 한 번"으로 같기 때문이다.
 * 합치지 않으면 좋아요 N회가 참가자 M명에게 N×M 이벤트로 증폭되고, 그만큼 재조회 요청이 발생한다.
 */
@Component
@RequiredArgsConstructor
public class PickeatUpdateBuffer {

    private static final long FLUSH_INTERVAL_MS = 100;

    private final Map<String, AtomicBoolean> pendingSlots = new ConcurrentHashMap<>();
    private final SseEmitterRegistry sseEmitterRegistry;

    /**
     * 슬롯을 세운다. 이미 세워져 있으면 그대로 덮어쓴다 -- 이것이 '합침'이다.
     */
    public void mark(String pickeatCode) {
        pendingSlots.computeIfAbsent(pickeatCode, code -> new AtomicBoolean())
                .set(true);
    }

    @Scheduled(fixedDelay = FLUSH_INTERVAL_MS)
    void flush() {
        for (String pickeatCode : pendingSlots.keySet()) {
            AtomicBoolean slot = pendingSlots.get(pickeatCode);
            if (slot == null) {
                continue;
            }

            // getAndSet 으로 '읽고 비우기'를 원자적으로 처리한다.
            // get() 후 set(false) 로 나누면 그 사이에 들어온 신호가 유실된다.
            if (slot.getAndSet(false)) {
                sseEmitterRegistry.broadcast(pickeatCode);
            } else if (!sseEmitterRegistry.hasEmitters(pickeatCode)) {
                // 구독자가 사라진 방의 슬롯을 정리한다. 신호가 다시 세워졌다면 남겨 둔다.
                pendingSlots.computeIfPresent(pickeatCode, (code, current) -> current.get() ? current : null);
            }
        }
    }
}
