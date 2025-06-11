package io.hhplus.tdd.point;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {

    private final static Long MAX_POINT = 100_000L;

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }

    public UserPoint use(long amount) {
        if (amount > this.point) {
            throw new IllegalArgumentException("기존 포인트보다 사용하려는 포인트가 더 많습니다.");
        } else if (amount == 0) {
            throw new IllegalArgumentException("사용할 포인트가 없습니다.");
        }

        return new UserPoint(this.id, this.point - amount, System.currentTimeMillis());
    }

    public UserPoint charge(long amount) {
        if (MAX_POINT < this.point + amount) {
            throw new IllegalArgumentException("충전하려는 포인트가 초과되었습니다.");
        } else if (amount == 0) {
            throw new IllegalArgumentException("충전 할 포인트가 없습니다.");
        }

        return new UserPoint(this.id, this.point + amount, System.currentTimeMillis());
    }
}
