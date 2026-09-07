package com.fledge.region.service;

import com.fledge.region.domain.Sido;
import com.fledge.region.repository.SidoRepository;
import com.fledge.region.repository.SigunguRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// "경기도 수원시 팔달구" 처럼 회원의 지역 코드를 사람이 읽을 수 있는 이름으로 바꾼다.
// member.regionCode/regionSigunguCode(현재 거주지, 매칭에 쓰는 값)를 위한 것이다 —
// home_region_code(보호종료 당시 거주지, 정착금 계산용)와는 다른 필드이니 섞어 쓰지 않는다.
@Component
@RequiredArgsConstructor
public class RegionNameResolver {
    private final SidoRepository sidoRepository;
    private final SigunguRepository sigunguRepository;

    public String resolve(String sidoCode, String sigunguCode) {
        if (sidoCode == null) return null;
        Sido sido = sidoRepository.findById(sidoCode).orElse(null);
        if (sido == null) return null;
        if (sigunguCode != null) {
            String sigunguName = sigunguRepository.findById(sigunguCode)
                    .map(sg -> sg.getName())
                    .orElse(null);
            if (sigunguName != null) return sido.getName() + " " + sigunguName;
        }
        return sido.getName();
    }
}
