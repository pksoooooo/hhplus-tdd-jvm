package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.springframework.stereotype.Service;

import java.util.List;

import static io.hhplus.tdd.point.TransactionType.CHARGE;
import static io.hhplus.tdd.point.TransactionType.USE;

@Service
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public PointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    public UserPoint findUserPointByUserId(Long userId) {
        return userPointTable.selectById(userId);
    }

    public List<PointHistory> findUserPointHistory(Long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }

    public synchronized UserPoint chargeUserPoint(Long userId, Long point) {

        UserPoint currentUserPoint = userPointTable.selectById(userId);
        UserPoint validUserPoint = currentUserPoint.charge(point);

        UserPoint userPoint = userPointTable.insertOrUpdate(userId, validUserPoint.point());

        pointHistoryTable.insert(userId, point, CHARGE, System.currentTimeMillis());

        return userPoint;
    }

    public synchronized UserPoint usePoint(Long userId, Long point) {

        UserPoint currentPoint = userPointTable.selectById(userId);
        UserPoint validUserPoint = currentPoint.use(point);

        UserPoint resultUserPoint = userPointTable.insertOrUpdate(validUserPoint.id(), validUserPoint.point());

        pointHistoryTable.insert(resultUserPoint.id(), point, USE, System.currentTimeMillis());

        return resultUserPoint;
    }

}
