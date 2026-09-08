package com.fledge.benefit.service;

import com.fledge.benefit.domain.Subsidy;
import com.fledge.benefit.domain.SubsidyRegion;
import com.fledge.benefit.dto.SubsidySummaryResponse;
import com.fledge.benefit.repository.SubsidyRegionRepository;
import com.fledge.benefit.repository.SubsidyRepository;
import com.fledge.member.domain.Member;
import com.fledge.member.repository.MemberRepository;
import com.fledge.region.service.RegionNameResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubsidyService {

    private static final int SEARCH_LIMIT = 20;
    // 정렬(회원 지역 우선) 전에 넉넉히 가져올 개수. 카탈로그 전체(55개 안팎)보다 크게 잡는다
    private static final int SEARCH_FETCH_LIMIT = 100;

    private final SubsidyRepository subsidyRepository;
    private final SubsidyRegionRepository subsidyRegionRepository;
    private final RegionNameResolver regionNameResolver;
    private final MemberRepository memberRepository;

    // "받고 있는 지원금 추가" 화면에서 이름으로 검색. 우리 카탈로그 안에서만 찾고,
    // 이름이 같은 지역별 지원금이 여러 개일 때 회원의 현재 지역과 일치하는 것을 위로 올린다.
    // 같은 이름+같은 지역 지원금이 여러 소스에서 중복 수집된 경우 하나만 보여준다
    public List<SubsidySummaryResponse> search(String query, Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        List<Subsidy> matches = subsidyRepository.findByNameContainingIgnoreCase(query, PageRequest.of(0, SEARCH_FETCH_LIMIT));

        List<Map.Entry<Subsidy, List<SubsidyRegion>>> deduped = dedupeByNameAndRegion(matches);

        return deduped.stream()
                .sorted(Comparator.comparing((Map.Entry<Subsidy, List<SubsidyRegion>> e) -> matchesMemberRegion(e.getValue(), member)).reversed())
                .limit(SEARCH_LIMIT)
                .map(e -> SubsidySummaryResponse.from(e.getKey(), regionLabelOf(e.getValue())))
                .toList();
    }

    private List<Map.Entry<Subsidy, List<SubsidyRegion>>> dedupeByNameAndRegion(List<Subsidy> subsidies) {
        Map<String, Map.Entry<Subsidy, List<SubsidyRegion>>> deduped = new LinkedHashMap<>();
        for (Subsidy s : subsidies) {
            List<SubsidyRegion> regions = subsidyRegionRepository.findBySubsidy_Id(s.getId());
            String regionLabel = regionLabelOf(regions);
            String key = s.getName() + " " + (regionLabel == null ? "" : regionLabel);
            Map.Entry<Subsidy, List<SubsidyRegion>> existing = deduped.get(key);
            if (existing == null || (existing.getKey().getOrgName() == null && s.getOrgName() != null)) {
                deduped.put(key, new AbstractMap.SimpleEntry<>(s, regions));
            }
        }
        return List.copyOf(deduped.values());
    }

    // "받고 있는 지원금" 목록에서도 같은 이름 구분이 필요해 search()와 공유한다
    public List<SubsidySummaryResponse> summarize(List<Subsidy> subsidies) {
        return subsidies.stream()
                .map(s -> SubsidySummaryResponse.from(s, regionLabelOf(subsidyRegionRepository.findBySubsidy_Id(s.getId()))))
                .toList();
    }

    // BenefitMatchingService의 지역 조건 판정과 같은 규칙: 시군구가 있으면 시군구로, 없으면 시도로 비교
    private boolean matchesMemberRegion(List<SubsidyRegion> regions, Member member) {
        return regions.stream().anyMatch(r ->
                r.getSigunguCode() != null
                        ? r.getSigunguCode().equals(member.getRegionSigunguCode())
                        : r.getSidoCode().equals(member.getRegionCode()));
    }

    private String regionLabelOf(List<SubsidyRegion> regions) {
        if (regions.isEmpty()) return null;
        return regions.stream()
                .map(r -> regionNameResolver.resolve(r.getSidoCode(), r.getSigunguCode()))
                .filter(name -> name != null)
                .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new),
                        names -> names.isEmpty() ? null : String.join("·", names)));
    }
}
