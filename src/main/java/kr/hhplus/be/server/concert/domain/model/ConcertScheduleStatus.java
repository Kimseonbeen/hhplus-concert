package kr.hhplus.be.server.concert.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ConcertScheduleStatus {
    AVAILABLE("예약가능"),
    SOLDOUT("매진");

    private final String description;

}
