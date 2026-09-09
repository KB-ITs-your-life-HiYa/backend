package com.fledge.counselor.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "counselor")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Counselor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long memberId;
    private String name;
    private String phone;
    private String organization;
    private String regionCode;
    @Column(name = "is_active")
    private boolean active;
}
