package io.hhplus.tdd;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.service.PointService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@SpringBootTest
public class PointIntegratedTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private UserPointTable userPointTable;

    @Autowired
    private PointHistoryTable pointHistoryTable;

    @AfterEach
    void reset() {
        Map<?, ?> userPointData = (Map<?, ?>) ReflectionTestUtils.getField(userPointTable, "table");
        if (userPointData != null) {
            userPointData.clear();
        }

        List<?> pointHistoryData = (List<?>) ReflectionTestUtils.getField(pointHistoryTable, "table");
        if (pointHistoryData != null) {
            pointHistoryData.clear();
        }
    }

    @Test
    void 동시에_100개의_포인트_충전_요청이_들어올_때_정상_처리되는지() throws InterruptedException {

        int countDownSize = 10;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(countDownSize);

        // given
        Long userId = 1L;
        Long chargePoint = 3000L;

        // when
        for (int i = 0; i < countDownSize; i++) {
            executor.submit(() -> {
                try {
                    pointService.chargeUserPoint(userId, chargePoint);
                } catch (Exception e) {
                    System.out.println("에러: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드 작업이 완료될 때까지 대기

        // then
        long finalPoint = pointService.findUserPointByUserId(userId).point();

        assertThat(30_000L).isEqualTo(finalPoint);
    }

    @Test
    void 동시에_100개의_포인트_사용_요청이_들어올_때_정상_처리되는지() throws InterruptedException {

        int countDownSize = 10;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(countDownSize);

        // given
        Long userId = 1L;
        Long usePoint = 1_000L;

        // when
        pointService.chargeUserPoint(userId, 100_000L);

        for (int i = 0; i < countDownSize; i++) {
            executor.submit(() -> {
                try {
                    pointService.usePoint(1L, usePoint); // 100원씩 차감 시도
                } catch (Exception e) {
                    System.out.println("에러: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드 작업이 완료될 때까지 대기

        // then
        long finalPoint = pointService.findUserPointByUserId(userId).point();
        System.out.println("최종 포인트: " + finalPoint);

        assertThat(finalPoint).isEqualTo(90_000L);
    }

    @Test
    void 여러건의_충전_사용_요청이_들어올_때_정상_처리되는지() throws InterruptedException {

        int countDownSize = 10;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(countDownSize);

        // given
        long userId = 1L;
        long defaultAmout = 10_000L;
        long usePoint = 100L;
        long chargePoint = 100L;

        // when
        pointService.chargeUserPoint(userId, 10_000L);

        for (int i = 0; i < countDownSize; i++) {
            int taskNum = i;
            executor.submit(() -> {
                try {
                    if(taskNum % 2 == 0){
                        pointService.usePoint(1L, usePoint); // 100원씩 차감 시도
                    }else{
                        pointService.chargeUserPoint(1L, chargePoint); // 100원씩 차감 시도
                    }

                } catch (Exception e) {
                    System.out.println("에러: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드 작업이 완료될 때까지 대기

        // then
        long finalPoint = pointService.findUserPointByUserId(userId).point();
        System.out.println("최종 포인트: " + finalPoint);

        List<PointHistory> userPointHistory = pointService.findUserPointHistory(userId);
        System.out.println("userPointHistory = " + userPointHistory);

        assertThat(finalPoint).isEqualTo(10_000L);
    }

    @Test
    void 포인트_충전() {

        long userId = 1L;
        long amount = 1000L;

        UserPoint result = pointService.chargeUserPoint(userId, amount);

        assertThat(result.point()).isEqualTo(amount);
    }

    @Test
    void 포인트_사용() {

        long userId = 1L;
        long chargeAmount = 1000L;
        long useAmount = 100L;

        pointService.chargeUserPoint(userId, chargeAmount);
        pointService.usePoint(userId, useAmount);

        UserPoint result = pointService.findUserPointByUserId(userId);

        assertThat(result.point()).isEqualTo(chargeAmount - useAmount);
    }

}
