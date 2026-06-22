package com.seohamin.money.global.support;

import com.seohamin.money.global.config.OpenBankingProperties;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * bank_tran_id(AN20) 생성기. 포맷 = 이용기관코드(10자리) + "U" + 일련번호(9자리).
 * 일련번호는 호출마다 증가하며, 재시작 시 0부터 시작하지 않도록 시간 기반으로 시드한다.
 */
@Component
public class BankTranIdGenerator {

    private static final long SEQ_MOD = 1_000_000_000L; // 10^9, 일련번호 9자리 wrap

    private final String clientUseCode;
    private final AtomicLong counter;

    public BankTranIdGenerator(OpenBankingProperties properties) {
        this.clientUseCode = properties.clientUseCode();
        this.counter = new AtomicLong(Math.floorMod(System.nanoTime(), SEQ_MOD));
    }

    public String generate() {
        long seq = Math.floorMod(counter.getAndIncrement(), SEQ_MOD);
        return clientUseCode + "U" + "%09d".formatted(seq);
    }
}
