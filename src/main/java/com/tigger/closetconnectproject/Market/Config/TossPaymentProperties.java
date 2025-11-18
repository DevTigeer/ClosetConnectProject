package com.tigger.closetconnectproject.Market.Config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 토스페이먼츠 설정 프로퍼티
 * application.properties에서 toss.payments.* 값을 자동으로 바인딩
 *
 * 사용법:
 * 1. application.properties에 아래 값 입력:
 *    toss.payments.client-key=test_ck_...
 *    toss.payments.secret-key=test_sk_...
 *
 * 2. 환경 변수로 오버라이드 가능:
 *    export TOSS_PAYMENTS_CLIENT_KEY=test_ck_...
 *    export TOSS_PAYMENTS_SECRET_KEY=test_sk_...
 */
@Component
@ConfigurationProperties(prefix = "toss.payments")
@Getter
@Setter
public class TossPaymentProperties {

    /**
     * 토스페이먼츠 클라이언트 키 (공개키)
     * 브라우저에서 사용하는 공개 API 키
     *
     * 발급 방법:
     * 1. https://developers.tosspayments.com/ 접속
     * 2. 로그인 후 "개발자센터" → "API 키" 메뉴
     * 3. 테스트 모드에서 "클라이언트 키" 복사
     */
    private String clientKey;

    /**
     * 토스페이먼츠 시크릿 키 (비밀키)
     * 서버에서 결제 승인 API 호출 시 사용
     * ⚠️ 절대 클라이언트에 노출되면 안됨!
     *
     * 발급 방법:
     * 1. https://developers.tosspayments.com/ 접속
     * 2. 로그인 후 "개발자센터" → "API 키" 메뉴
     * 3. 테스트 모드에서 "시크릿 키" 복사
     */
    private String secretKey;

    /**
     * 토스페이먼츠 API URL
     * 기본값: https://api.tosspayments.com/v1
     */
    private String apiUrl = "https://api.tosspayments.com/v1";

    /**
     * 결제 성공 시 리다이렉트될 URL
     * 기본값: http://localhost:8080/payment-success.html
     */
    private String successUrl = "http://localhost:8080/payment-success.html";

    /**
     * 결제 실패 시 리다이렉트될 URL
     * 기본값: http://localhost:8080/payment-fail.html
     */
    private String failUrl = "http://localhost:8080/payment-fail.html";

    /**
     * 시크릿 키의 Base64 인코딩 (Authorization 헤더에 사용)
     * 토스 API는 Basic Auth 방식: "Basic " + Base64(secretKey + ":")
     */
    public String getEncodedSecretKey() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("토스페이먼츠 시크릿 키가 설정되지 않았습니다. application.properties에 toss.payments.secret-key를 설정하세요.");
        }
        return java.util.Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes());
    }

    /**
     * 설정 유효성 검증
     */
    public void validate() {
        if (clientKey == null || clientKey.isBlank()) {
            throw new IllegalStateException("토스페이먼츠 클라이언트 키가 설정되지 않았습니다.");
        }
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("토스페이먼츠 시크릿 키가 설정되지 않았습니다.");
        }
        if (apiUrl == null || apiUrl.isBlank()) {
            throw new IllegalStateException("토스페이먼츠 API URL이 설정되지 않았습니다.");
        }
    }

    /**
     * 테스트 모드 여부 확인
     * 클라이언트 키가 "test_"로 시작하면 테스트 모드
     */
    public boolean isTestMode() {
        return clientKey != null && clientKey.startsWith("test_");
    }
}
