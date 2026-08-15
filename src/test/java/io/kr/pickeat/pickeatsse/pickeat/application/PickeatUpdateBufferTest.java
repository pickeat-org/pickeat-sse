package io.kr.pickeat.pickeatsse.pickeat.application;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("픽잇 갱신 신호 버퍼")
class PickeatUpdateBufferTest {

    private static final String PICKEAT_CODE = "ABCD1234";

    private final SseEmitterRegistry sseEmitterRegistry = mock(SseEmitterRegistry.class);
    private final PickeatUpdateBuffer pickeatUpdateBuffer = new PickeatUpdateBuffer(sseEmitterRegistry);

    @Nested
    class 신호_합침_케이스 {

        @Test
        void 여러_번_들어온_신호를_한_번의_전송으로_합친다() {
            // given
            for (int i = 0; i < 100; i++) {
                pickeatUpdateBuffer.mark(PICKEAT_CODE);
            }

            // when
            pickeatUpdateBuffer.flush();

            // then
            then(sseEmitterRegistry).should(times(1)).broadcast(PICKEAT_CODE);
        }

        @Test
        void 신호가_없으면_전송하지_않는다() {
            // when
            pickeatUpdateBuffer.flush();

            // then
            then(sseEmitterRegistry).should(never()).broadcast(PICKEAT_CODE);
        }

        @Test
        void 비운_뒤_다시_들어온_신호는_다음_전송에_반영된다() {
            // given
            pickeatUpdateBuffer.mark(PICKEAT_CODE);
            pickeatUpdateBuffer.flush();

            // when
            pickeatUpdateBuffer.mark(PICKEAT_CODE);
            pickeatUpdateBuffer.flush();

            // then
            then(sseEmitterRegistry).should(times(2)).broadcast(PICKEAT_CODE);
        }

        @Test
        void 한_번_보낸_뒤_새_신호가_없으면_다시_보내지_않는다() {
            // given
            pickeatUpdateBuffer.mark(PICKEAT_CODE);
            pickeatUpdateBuffer.flush();

            // when
            pickeatUpdateBuffer.flush();
            pickeatUpdateBuffer.flush();

            // then
            then(sseEmitterRegistry).should(times(1)).broadcast(PICKEAT_CODE);
        }

        @Test
        void 방마다_슬롯이_분리되어_서로_영향을_주지_않는다() {
            // given
            pickeatUpdateBuffer.mark(PICKEAT_CODE);
            pickeatUpdateBuffer.mark("OTHERROOM");

            // when
            pickeatUpdateBuffer.flush();

            // then
            then(sseEmitterRegistry).should(times(1)).broadcast(PICKEAT_CODE);
            then(sseEmitterRegistry).should(times(1)).broadcast("OTHERROOM");
        }
    }

    @Nested
    class 슬롯_정리_케이스 {

        @Test
        void 구독자가_없어진_방의_슬롯은_정리한다() {
            // given
            pickeatUpdateBuffer.mark(PICKEAT_CODE);
            pickeatUpdateBuffer.flush();
            given(sseEmitterRegistry.hasEmitters(PICKEAT_CODE)).willReturn(false);

            // when
            pickeatUpdateBuffer.flush();
            pickeatUpdateBuffer.flush();

            // then
            then(sseEmitterRegistry).should(times(1)).hasEmitters(PICKEAT_CODE);
        }

        @Test
        void 구독자가_남아_있으면_슬롯을_유지한다() {
            // given
            pickeatUpdateBuffer.mark(PICKEAT_CODE);
            pickeatUpdateBuffer.flush();
            given(sseEmitterRegistry.hasEmitters(PICKEAT_CODE)).willReturn(true);

            // when
            pickeatUpdateBuffer.flush();
            pickeatUpdateBuffer.flush();

            // then
            then(sseEmitterRegistry).should(times(2)).hasEmitters(PICKEAT_CODE);
        }
    }
}
