package kr.hhplus.be.server.user.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Builder
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class User {

    @Id @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 생성 메서드
    public static User createUser() {
        return new User();
    }
}
