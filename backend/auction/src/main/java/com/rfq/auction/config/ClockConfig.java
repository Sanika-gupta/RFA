package com.rfq.auction.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    /** Single source of "now". Tests replace it with Clock.fixed(...) to simulate any time. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
