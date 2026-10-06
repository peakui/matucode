package com.peakui.gateway.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IpAddressRulesTest {
    @Test
    void matchesExactIpAndCidrWithoutWildcardOrDns() {
        var rules = IpAddressRules.parse("203.0.113.0/24,2001:db8::1");
        assertTrue(IpAddressRules.matches(rules, "203.0.113.44"));
        assertTrue(IpAddressRules.matches(rules, "2001:db8::1"));
        assertFalse(IpAddressRules.matches(rules, "203.0.114.1"));
        assertThrows(IllegalArgumentException.class, () -> IpAddressRules.parse("example.com"));
    }

    @Test
    void rejectsMalformedCidr() {
        assertThrows(IllegalArgumentException.class, () -> IpAddressRules.parse("10.0.0.0/33"));
        assertThrows(IllegalArgumentException.class, () -> IpAddressRules.parse("10.0.0.0/24/1"));
    }
}
