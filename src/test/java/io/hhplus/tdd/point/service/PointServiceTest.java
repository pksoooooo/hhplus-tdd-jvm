package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.hhplus.tdd.point.TransactionType.CHARGE;
import static io.hhplus.tdd.point.TransactionType.USE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    @InjectMocks
    private PointService pointService;

    @Mock
    PointHistoryTable pointHistoryTable;

    @Mock
    UserPointTable userPointTable;

    @Test
    @DisplayName("포인트를 충전 한다")
    void 포인트충전() {

        // given
        Long userId = 1L;
        Long chargePoint = 10000L;

        UserPoint beforeCharge = new UserPoint(userId, chargePoint, System.currentTimeMillis());
        UserPoint afterCharge = new UserPoint(userId, beforeCharge.point() + chargePoint, System.currentTimeMillis());

        given(userPointTable.selectById(anyLong())).willReturn(beforeCharge);
        given(userPointTable.insertOrUpdate(anyLong(), anyLong())).willReturn(afterCharge);

        // when
        UserPoint userPoint = pointService.chargeUserPoint(userId, chargePoint);

        // then
        assertThat(userPoint.point()).isEqualTo(afterCharge.point());

    }

    @Test
    @DisplayName("충전하려는 포인트가 최대 충전 포인트를 초과 한다.")
    void 포인트충전초과() {

        // given
        Long userId = 1L;
        Long chargePoint = 100_000L;

        UserPoint beforeUserPoint = new UserPoint(userId, chargePoint, System.currentTimeMillis());
        given(userPointTable.selectById(anyLong())).willReturn(beforeUserPoint);

        // then
        assertThatThrownBy(() -> pointService.chargeUserPoint(userId, chargePoint))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("충전하려는 포인트가 초과되었습니다.");

    }

    @Test
    @DisplayName("포인트 충전 내역을 확인 한다.")
    void 포인트충전내역확인() {

        // given
        long userId = 1;

        ArrayList<PointHistory> pointHistoryList = new ArrayList<>();
        PointHistory pointHistory1 = new PointHistory(1, userId, 10000, CHARGE, System.currentTimeMillis());
        PointHistory pointHistory2 = new PointHistory(2, userId, 10000, CHARGE, System.currentTimeMillis());
        PointHistory pointHistory3 = new PointHistory(3, userId, 10000, CHARGE, System.currentTimeMillis());

        pointHistoryList.add(pointHistory1);
        pointHistoryList.add(pointHistory2);
        pointHistoryList.add(pointHistory3);

        given(pointHistoryTable.selectAllByUserId(anyLong())).willReturn(pointHistoryList);

        // when
        List<PointHistory> pointHistories = pointService.findUserPointHistory(userId);

        // then
        assertThat(pointHistoryList.size()).isEqualTo(pointHistories.size());

    }

    @Test
    @DisplayName("포인트를 사용한다.")
    void 포인트사용() {

        //given
        long userId = 1L;
        long usePoint = 10_000L;

        UserPoint beforePoint = new UserPoint(userId, 100_000L, System.currentTimeMillis());
        UserPoint afterPoint = new UserPoint(userId, 100_000L - usePoint, System.currentTimeMillis());

        given(userPointTable.selectById(anyLong())).willReturn(beforePoint);
        given(userPointTable.insertOrUpdate(anyLong(), anyLong())).willReturn(afterPoint);

        //when
        UserPoint userPoint = pointService.usePoint(userId, usePoint);

        //then
        assertThat(userPoint.point()).isEqualTo(afterPoint.point());

    }

    @Test
    @DisplayName("기존 포인트 보다 사용 포인트가 큰 경우")
    void 기존포인트보다사용포인트가큰경우() {

        //given
        long userId = 1L;
        long chargePoint = 100_000L;
        long usePoint = 190_000L;

        UserPoint userPoint = new UserPoint(userId, chargePoint, System.currentTimeMillis());
        given(userPointTable.selectById(anyLong())).willReturn(userPoint);

        //then
        assertThatThrownBy(() -> pointService.usePoint(userId, usePoint))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("기존 포인트보다 사용하려는 포인트가 더 많습니다.");

    }

    @Test
    @DisplayName("현재 포인트를 반환한다.")
    void 포인트조회() {

        // given
        long userId = 1L;
        long point = 10_000L;

        UserPoint mockUserPoint = new UserPoint(userId, point, System.currentTimeMillis());

        given(userPointTable.selectById(anyLong())).willReturn(mockUserPoint);

        // when
        UserPoint userPointByUserId = pointService.findUserPointByUserId(userId);

        // then
        assertThat(userPointByUserId.point()).isEqualTo(mockUserPoint.point());

    }

    @Test
    @DisplayName("사용자 포인트 내역을 조회 한다.")
    void 포인트사용내역() {

        //given
        Long userId = 1L;
        Long point = 10000L;

        PointHistory pointHistory1 = new PointHistory(1, userId, point, CHARGE, System.currentTimeMillis());
        PointHistory pointHistory2 = new PointHistory(2, userId, point, USE, System.currentTimeMillis());
        PointHistory pointHistory3 = new PointHistory(3, userId, point, CHARGE, System.currentTimeMillis());
        PointHistory pointHistory4 = new PointHistory(4, userId, point, USE, System.currentTimeMillis());

        List<PointHistory> pointHistories = List.of(pointHistory1, pointHistory2, pointHistory3, pointHistory4);

        given(pointHistoryTable.selectAllByUserId(anyLong())).willReturn(pointHistories);

        //when
        List<PointHistory> userPointHistory = pointService.findUserPointHistory(userId);

        //then
        assertThat(userPointHistory.size()).isEqualTo(pointHistories.size());
    }





}
