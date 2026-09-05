package com.fledge.region.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name="sido")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sido {
    @Id
    private String code;

    private String name;
}
