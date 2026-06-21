package com.seohamin.money.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing(@CreatedDate/@LastModifiedDate) 활성화 설정. 테스트에서도 활성화하기 위해 따로 분리.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
