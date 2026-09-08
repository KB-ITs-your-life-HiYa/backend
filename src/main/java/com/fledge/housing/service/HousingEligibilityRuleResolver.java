package com.fledge.housing.service;

import com.fledge.housing.domain.HousingEligibilityRuleType;
import com.fledge.housing.domain.HousingNotice;
import com.fledge.housing.domain.TargetType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HousingEligibilityRuleResolver {
    private static final List<String> YOUTH_PURCHASE_EXCLUSIONS = List.of(
            "신혼", "신생아", "리츠", "기숙사형", "천원주택", "자격완화");

    public HousingEligibilityRuleType resolve(HousingNotice notice) {
        if (isSelfRelianceDemo(notice)) {
            return HousingEligibilityRuleType.SELF_RELIANCE_DEMO;
        }
        if (isLhYouthPurchase(notice)) {
            return HousingEligibilityRuleType.LH_YOUTH_PURCHASE;
        }
        return HousingEligibilityRuleType.UNSUPPORTED;
    }

    private boolean isSelfRelianceDemo(HousingNotice notice) {
        return "SEED-SR-001".equals(notice.getPblancId())
                || "SEED-SR-002".equals(notice.getPblancId());
    }

    private boolean isLhYouthPurchase(HousingNotice notice) {
        if (!"LH".equalsIgnoreCase(trim(notice.getSuplyInsttNm()))
                || notice.getTargetType() != TargetType.YOUTH
                || !"매입임대".equals(trim(notice.getSuplyTyNm()))) {
            return false;
        }
        String compactTitle = trim(notice.getPblancNm()).replaceAll("\\s+", "");
        if (!compactTitle.contains("청년매입임대")) {
            return false;
        }
        return YOUTH_PURCHASE_EXCLUSIONS.stream().noneMatch(compactTitle::contains);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
