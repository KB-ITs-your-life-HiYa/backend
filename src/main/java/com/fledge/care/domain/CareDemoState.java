package com.fledge.care.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
public class CareDemoState {
    @Id private Long memberId;
    private OffsetDateTime asOf;
}
