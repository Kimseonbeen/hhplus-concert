package kr.hhplus.be.server.user.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String name;
}
