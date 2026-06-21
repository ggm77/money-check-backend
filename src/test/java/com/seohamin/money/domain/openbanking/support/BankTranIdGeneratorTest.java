package com.seohamin.money.domain.openbanking.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.seohamin.money.global.config.OpenBankingProperties;
import org.junit.jupiter.api.Test;

class BankTranIdGeneratorTest {

    private static final String USE_CODE = "M202500001"; // 10자리

    private final BankTranIdGenerator generator = new BankTranIdGenerator(
            new OpenBankingProperties("http://x", "cid", "secret", "http://cb", USE_CODE, "login inquiry"));

    @Test
    void generatesTwentyCharIdWithExpectedFormat() {
        String id = generator.generate();

        assertThat(id).hasSize(20);
        assertThat(id).startsWith(USE_CODE);
        assertThat(id.charAt(10)).isEqualTo('U');
        assertThat(id.substring(11)).matches("\\d{9}"); // 일련번호 9자리
    }

    @Test
    void generatesUniqueSequentialIds() {
        String first = generator.generate();
        String second = generator.generate();

        assertThat(first).isNotEqualTo(second);
    }
}
