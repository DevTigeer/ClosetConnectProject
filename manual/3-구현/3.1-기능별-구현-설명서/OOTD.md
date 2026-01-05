# OOTD 기능 구현 설명서

**작성일**: 2024-12-30
**작성자**: Development Team
**버전**: 1.0

---

## 📌 요약

OOTD(Outfit Of The Day)는 AI 코디 조합 결과를 저장하고 관리하는 기능으로, 사용자가 마음에 드는 코디를 갤러리 형식으로 보관할 수 있습니다.

---

## 1. 기능 설명

### 목적
- AI로 생성한 코디 이미지를 저장
- 저장된 OOTD를 갤러리 형식으로 관리
- 다운로드, 삭제 기능 제공

### 사용자 흐름
1. "내 옷장" → "조합하기" 클릭
2. 옷 선택 (상의, 하의, 신발, 액세서리)
3. "조합 생성하기" 클릭
4. AI 코디 이미지 생성
5. **"OOTD 저장" 클릭** ← 신규 기능
6. "OOTD" 메뉴에서 저장된 코디 확인

---

## 2. 주요 클래스/모듈

### 2.1 Backend (Java)

#### Ootd.java (Entity)
**위치**: `/src/main/java/com/tigger/closetconnectproject/Closet/Entity/Ootd.java`

```java
@Entity
public class Ootd extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Column(length = 100)
    private String description;
}
```

**필드 설명**:
- `id`: OOTD 고유 ID
- `user`: 소유자 (Users 엔티티 참조)
- `imageUrl`: AI 생성 코디 이미지 URL
- `description`: 코디 설명 (선택 사항)
- `createdAt`, `updatedAt`: BaseTimeEntity에서 상속

#### OotdRepository.java
**위치**: `/src/main/java/com/tigger/closetconnectproject/Closet/Repository/OotdRepository.java`

```java
public interface OotdRepository extends JpaRepository<Ootd, Long> {
    List<Ootd> findByUserUserIdOrderByCreatedAtDesc(Long userId);
}
```

**쿼리 메서드**:
- `findByUserUserIdOrderByCreatedAtDesc`: 특정 사용자의 OOTD를 최신 순으로 조회

#### OotdService.java
**위치**: `/src/main/java/com/tigger/closetconnectproject/Closet/Service/OotdService.java`

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OotdService {

    @Transactional
    public OotdDtos.Response save(Long userId, OotdDtos.CreateRequest request) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Ootd ootd = Ootd.builder()
                .user(user)
                .imageUrl(request.imageUrl())
                .description(request.description())
                .build();

        return OotdDtos.Response.from(ootdRepository.save(ootd));
    }

    public List<OotdDtos.Response> findByUserId(Long userId) {
        return ootdRepository.findByUserUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(OotdDtos.Response::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long ootdId, Long userId) {
        Ootd ootd = ootdRepository.findById(ootdId)
                .orElseThrow(() -> new IllegalArgumentException("OOTD를 찾을 수 없습니다."));

        if (!ootd.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        ootdRepository.delete(ootd);
    }
}
```

**메서드 설명**:
- `save`: OOTD 저장 (트랜잭션)
- `findByUserId`: 사용자별 OOTD 목록 조회
- `delete`: OOTD 삭제 (권한 검증)

#### OotdController.java
**위치**: `/src/main/java/com/tigger/closetconnectproject/Closet/Controller/OotdController.java`

```java
@RestController
@RequestMapping("/api/v1/ootd")
@RequiredArgsConstructor
public class OotdController {

    @PostMapping
    public ResponseEntity<OotdDtos.Response> save(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @Valid @RequestBody OotdDtos.CreateRequest request
    ) {
        OotdDtos.Response response = ootdService.save(
            userDetails.getUser().getUserId(),
            request
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<OotdDtos.Response>> getMyOotds(
            @AuthenticationPrincipal AppUserDetails userDetails
    ) {
        List<OotdDtos.Response> ootds = ootdService.findByUserId(
            userDetails.getUser().getUserId()
        );
        return ResponseEntity.ok(ootds);
    }

    @DeleteMapping("/{ootdId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long ootdId
    ) {
        ootdService.delete(ootdId, userDetails.getUser().getUserId());
        return ResponseEntity.noContent().build();
    }
}
```

**API 엔드포인트**:
- `POST /api/v1/ootd`: OOTD 저장
- `GET /api/v1/ootd`: 내 OOTD 목록 조회
- `DELETE /api/v1/ootd/:id`: OOTD 삭제

### 2.2 Frontend (React)

#### OOTDPage.jsx
**위치**: `/prontend/ClosetConnectProject/frontend/src/pages/OOTDPage.jsx`

**주요 기능**:
- OOTD 목록 조회 및 갤러리 표시
- 이미지 확대 모달
- 다운로드 기능
- 삭제 기능

**핵심 코드**:
```javascript
const fetchOotds = async () => {
  const response = await ootdAPI.list();
  setOotds(response.data);
};

const handleDelete = async (id) => {
  await ootdAPI.delete(id);
  setOotds(ootds.filter((ootd) => ootd.id !== id));
};

const handleDownload = async (imageUrl, id) => {
  const response = await fetch(fullImageUrl);
  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `ootd-${id}-${Date.now()}.png`;
  link.click();
};
```

#### OutfitTryonModal.jsx 수정사항
**위치**: `/prontend/ClosetConnectProject/frontend/src/components/OutfitTryonModal.jsx`

**추가된 기능**: OOTD 저장 버튼

```javascript
const handleSave = async () => {
  setSaving(true);
  await ootdAPI.save({
    imageUrl: result.imageUrl,
    description: null,
  });
  alert('OOTD가 저장되었습니다! 🎉');
  setSaving(false);
};

// UI
<button className="btn-primary" onClick={handleSave} disabled={saving}>
  {saving ? '저장 중...' : '💾 OOTD 저장'}
</button>
```

---

## 3. 흐름 설명

### 3.1 OOTD 저장 흐름

```
1. 사용자: "조합 생성하기" 클릭
   ↓
2. AI 코디 이미지 생성
   ↓
3. 결과 화면에서 "OOTD 저장" 버튼 클릭
   ↓
4. React → POST /api/v1/ootd
   {
     "imageUrl": "/uploads/outfit/result-123.png",
     "description": null
   }
   ↓
5. Spring Boot:
   - JWT 토큰에서 userId 추출
   - Ootd 엔티티 생성
   - DB 저장
   ↓
6. React: 성공 알림 표시
```

### 3.2 OOTD 조회 흐름

```
1. 사용자: "OOTD" 메뉴 클릭
   ↓
2. React → GET /api/v1/ootd
   ↓
3. Spring Boot:
   - JWT에서 userId 추출
   - findByUserUserIdOrderByCreatedAtDesc(userId)
   - 최신 순으로 정렬된 OOTD 목록 반환
   ↓
4. React: 갤러리 형식으로 렌더링
```

### 3.3 OOTD 삭제 흐름

```
1. 사용자: 삭제 버튼(🗑️) 클릭
   ↓
2. 확인 다이얼로그 표시
   ↓
3. React → DELETE /api/v1/ootd/:id
   ↓
4. Spring Boot:
   - OOTD 조회
   - 소유자 확인 (본인만 삭제 가능)
   - DB에서 삭제
   ↓
5. React: 목록에서 제거
```

---

## 4. 고려한 예외

### 예외 케이스 1: 권한 없는 삭제 시도
- **상황**: 다른 사용자의 OOTD를 삭제하려는 경우
- **대응**:
  ```java
  if (!ootd.getUser().getUserId().equals(userId)) {
      throw new IllegalArgumentException("삭제 권한이 없습니다.");
  }
  ```
- **결과**: 403 Forbidden

### 예외 케이스 2: 존재하지 않는 OOTD
- **상황**: 삭제된 OOTD ID로 요청
- **대응**:
  ```java
  ootdRepository.findById(ootdId)
      .orElseThrow(() -> new IllegalArgumentException("OOTD를 찾을 수 없습니다."));
  ```
- **결과**: 404 Not Found

### 예외 케이스 3: 네트워크 오류
- **상황**: API 호출 실패
- **대응**: try-catch + 에러 메시지
  ```javascript
  try {
    await ootdAPI.save(...);
  } catch (err) {
    alert('OOTD 저장에 실패했습니다.');
  }
  ```

---

## 5. 테스트 방법

### 5.1 Backend 테스트 (수동)

1. **저장 테스트**
   ```bash
   curl -X POST http://localhost:8080/api/v1/ootd \
     -H "Authorization: Bearer YOUR_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"imageUrl":"/uploads/test.png","description":"테스트"}'
   ```

2. **조회 테스트**
   ```bash
   curl -X GET http://localhost:8080/api/v1/ootd \
     -H "Authorization: Bearer YOUR_TOKEN"
   ```

3. **삭제 테스트**
   ```bash
   curl -X DELETE http://localhost:8080/api/v1/ootd/1 \
     -H "Authorization: Bearer YOUR_TOKEN"
   ```

### 5.2 Frontend 테스트

1. 로그인 후 "내 옷장" 이동
2. "조합하기" 클릭하여 코디 생성
3. "OOTD 저장" 버튼 클릭
4. "OOTD" 메뉴로 이동하여 저장 확인
5. 다운로드 버튼 클릭하여 이미지 다운로드 확인
6. 삭제 버튼 클릭하여 삭제 확인

---

## 6. 향후 개선 사항

### Phase 2
- [ ] OOTD에 태그 추가 (#캐주얼, #데이트룩 등)
- [ ] OOTD 검색 기능
- [ ] OOTD 공유 기능 (커뮤니티 연동)
- [ ] OOTD 좋아요 기능

### Phase 3
- [ ] AI 기반 OOTD 추천
- [ ] 날씨별 OOTD 자동 추천
- [ ] OOTD 캘린더 (날짜별 착용 기록)

---

## 변경 이력

| 날짜 | 버전 | 작성자 | 변경 내용 |
|------|------|--------|-----------|
| 2024-12-30 | 1.0 | Development Team | OOTD 기능 구현 완료 및 문서 작성 |
