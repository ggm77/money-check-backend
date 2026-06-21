package com.seohamin.money.domain.openbanking.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OAuthStateStoreTest {

    private final OAuthStateStore stateStore = new OAuthStateStore();

    @Test
    void stateIsBoundToMemberAndCanOnlyBeConsumedOnce() {
        final String state = stateStore.issue(15L);

        assertThat(stateStore.consume(state)).contains(15L);
        assertThat(stateStore.consume(state)).isEmpty();
    }

    @Test
    void rejectsUnknownState() {
        assertThat(stateStore.consume("unknown")).isEmpty();
    }
}
