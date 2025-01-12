package kr.hhplus.be.server.balance.domain.model;

import jakarta.persistence.*;
import kr.hhplus.be.server.balance.domain.exception.BalanceError;
import kr.hhplus.be.server.balance.domain.exception.BalanceErrorCode;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Balance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    private Integer amount;

    public void decrease(Integer amount) {
        if (this.amount < amount) {
            throw new BalanceError(BalanceErrorCode.INSUFFICIENT_BALANCE);
        }
        this.amount -= amount;
    }

    public void increase(int amount) {
        if (amount <= 0) {
            throw new BalanceError(BalanceErrorCode.INVALID_AMOUNT);
        }
        this.amount += amount;
    }
}
