package com.fledge.benefit.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "subsidy_region")
public class SubsidyRegion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "subsidy_id", nullable = false)
    private Subsidy subsidy;

    private String sidoCode;
    private String sigunguCode;

    public SubsidyRegion(Subsidy subsidy, String sidoCode, String sigunguCode) {
        this.subsidy = subsidy;
        this.sidoCode = sidoCode;
        this.sigunguCode = sigunguCode;
    }

    protected SubsidyRegion() {}
}