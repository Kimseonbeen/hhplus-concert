package kr.hhplus.be.server.concert.domain;

import jakarta.persistence.*;

@Entity
public class Concert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String name;
}
