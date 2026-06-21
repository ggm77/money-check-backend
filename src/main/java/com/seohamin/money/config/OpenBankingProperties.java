package com.seohamin.money.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code openbanking.*} 설정 바인딩. 비밀값(clientId/clientSecret)과 환경별 base-url을 담는다.
 *
 * @param baseUrl 오픈뱅킹 API 호스트 (테스트베드: https://testapi.openbanking.or.kr)
 * @param clientId 이용기관 앱의 Client ID
 * @param clientSecret 이용기관 앱의 Client Secret
 * @param redirectUri 사용자인증 후 콜백받을 URI (이용기관 포털에 등록한 값과 동일해야 함)
 * @param clientUseCode 이용기관코드 10자리 (bank_tran_id 생성에 사용)
 * @param scope 요청 scope (예: "login inquiry")
 */
@ConfigurationProperties("openbanking")
public record OpenBankingProperties(
        String baseUrl,
        String clientId,
        String clientSecret,
        String redirectUri,
        String clientUseCode,
        String scope) {}
