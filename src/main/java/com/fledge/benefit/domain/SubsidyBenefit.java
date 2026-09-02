package com.fledge.benefit.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "subsidy_benefit")
public class SubsidyBenefit {

    @Id
    @GeneratedValue(strategy =  GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "subsidy_id", nullable = false)
    private Subsidy subsidy;

    private String benefitName;
    private Long amountKrw;
    private String cycle;

    public SubsidyBenefit(Subsidy subsidy, String benefitName, Long amountKrw, String cycle) {
        this.subsidy = subsidy;
        this.benefitName = benefitName;
        this.amountKrw = amountKrw;
        this.cycle = cycle;
    }

    protected SubsidyBenefit() {}
}
