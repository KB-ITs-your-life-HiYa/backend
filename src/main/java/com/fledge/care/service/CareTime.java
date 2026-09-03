package com.fledge.care.service;

import com.fledge.care.repository.CareDemoStateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import java.time.*;

@Component
public class CareTime {
    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    public static final LocalDate START = LocalDate.of(2026, 9, 23);
    private final CareDemoStateRepository states;
    private final boolean demoEnabled;

    public CareTime(CareDemoStateRepository states, Environment environment,
                    @Value("${care.demo.enabled:false}") boolean enabled) {
        this.states = states;
        this.demoEnabled = enabled && environment.acceptsProfiles(Profiles.of("local & !prod & !supabase"));
    }

    public boolean isDemo(Long memberId) { return demoEnabled && memberId == 2L; }

    public Clock clock(Long memberId) {
        if (!isDemo(memberId)) return Clock.system(ZONE);
        Instant instant = states.findById(memberId).map(s -> s.getAsOf().toInstant())
                .orElse(START.atTime(10, 0).atZone(ZONE).toInstant());
        return Clock.fixed(instant, ZONE);
    }

    public OffsetDateTime now(Long memberId) { return OffsetDateTime.now(clock(memberId)); }
}
