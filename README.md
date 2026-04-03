# TrendBridge

개인 기술 스택과 채용 공고 데이터를 연결해
사용자에게 맞는 공고를 추천하고 부족한 기술을 파악할 수 있도록 돕는 취업 지원 웹서비스입니다.

- GitHub: [https://github.com/hap728/KDT-4team-Project](https://github.com/hap728/KDT-4team-Project)

## 프로젝트 소개

최근 개발자 채용은 학력이나 스펙보다 실제 기술 스택 중심으로 빠르게 변화하고 있습니다.
하지만 취업 준비생은 자신의 현재 역량과 채용 시장이 요구하는 기술 사이의 차이를 파악하기 어렵습니다.

TrendBridge는 이 문제를 해결하기 위해 기획된 서비스로,
채용 공고 조회를 넘어 개인 기술 역량과 시장 요구를 연결해
취업 준비 방향을 보다 명확하게 제시하는 것을 목표로 합니다.

## 주요 기능

### 1. 회원 기능
- 일반 회원가입 / 로그인
- Google OAuth 로그인
- GitHub OAuth 로그인 및 계정 연동
- 세션 기반 로그인 상태 유지
- 회원탈퇴

### 2. 채용 공고 조회
- 직무 카테고리별 채용 공고 조회
- 페이지네이션 기반 공고 탐색
- 기업명 및 공고 정보 확인

### 3. 기술 트렌드 차트
- 카테고리별 기술 스택 통계 조회
- 특정 기술을 요구하는 공고 목록 확인

### 4. 기술 스택 기반 매칭
- 사용자 보유 기술 스택 기반 매칭 점수 계산
- 부족한 기술 스택 분석
- 점수 개선 시뮬레이션
- 추천 공고 제공

### 5. 프로필 관리
- 닉네임 / 자기소개 / 보유 기술 스택 수정
- 비밀번호 변경
- GitHub 연동 동의 및 연결 상태 확인

### 6. 북마크
- 관심 채용 공고 저장
- 저장한 공고 목록 조회 및 제거

## 기술 스택

### Backend
- Java 21
- Spring Boot
- Spring MVC
- Spring Security OAuth2 Client
- Thymeleaf
- MyBatis
- JPA

### Database
- Oracle DB

### Frontend
- HTML
- CSS
- JavaScript
- Thymeleaf Template

### Build / Tooling
- Gradle
- Maven 설정 파일 일부 포함

## 아키텍처 개요

이 프로젝트는 서버 렌더링 기반 MVC 구조를 바탕으로,
필요한 기능에 대해서는 REST API를 함께 사용하는 혼합형 구조입니다.

- `Controller`
  - 페이지 라우팅과 API 요청 처리
- `Service`
  - 로그인, 회원탈퇴, 북마크, 매칭 분석 등 비즈니스 로직 처리
- `Repository / Mapper`
  - DB 조회 및 저장 처리
- `Template + JavaScript`
  - 사용자 화면 렌더링 및 비동기 API 호출

## 디렉터리 구조

```text
src
└─ main
   ├─ java/com/project/mvcgithublogin
   │  ├─ config
   │  ├─ controller
   │  ├─ dao
   │  ├─ domain
   │  ├─ dto
   │  ├─ model
   │  ├─ profile
   │  ├─ repository
   │  └─ service
   └─ resources
      ├─ mapper
      ├─ static
      │  ├─ css
      │  └─ js
      └─ templates
```

## 주요 페이지

- `/` : 채용 공고 메인
- `/jobs` : 기술 트렌드 차트
- `/matching` : 매칭 분석
- `/matching-detail` : 세부 매칭 설정
- `/profile` : 프로필 조회
- `/profile-edit` : 프로필 수정
- `/bookmarks` : 북마크 공고 목록
- `/login` : 로그인
- `/signup` : 회원가입

## 주요 API 예시

### 사용자
- `POST /users/signup`
- `POST /users/login`
- `POST /users/logout`
- `GET /users/me`
- `DELETE /users/me`

### 프로필
- `GET /users/profile`
- `PUT /users/profile`
- `PUT /users/profile/github-consent`

### 채용 공고
- `GET /api/jobs?categoryId={id}&page={page}&size={size}`

### 북마크
- `GET /api/bookmarks`
- `GET /api/bookmarks/ids`
- `POST /api/bookmarks/{postingId}`
- `DELETE /api/bookmarks/{postingId}`

### GitHub 연동
- `GET /api/github/stacks`

## 실행 방법

### 1. 환경 확인
- JDK 21
- Oracle DB 연결 가능 환경
- `src/main/resources/application.properties` 설정 확인

### 2. 프로젝트 실행

```bash
./gradlew bootRun
```

Windows에서는 아래 명령으로 실행할 수 있습니다.

```powershell
.\gradlew.bat bootRun
```

실행 후 기본 접속 주소:

```text
http://localhost:8081
```

## 프로젝트 특징

- 채용 공고 조회에 그치지 않고 개인 기술 역량과 연결된 분석 제공
- GitHub 연동을 통한 기술 스택 확장 가능
- 서버 렌더링과 REST API를 혼합해 사용자 경험 개선
- MyBatis와 JPA를 기능 특성에 맞게 혼합 사용

## 개선 포인트

- 보안 설정 고도화
- 민감 정보의 환경 변수 분리
- MyBatis / JPA 사용 기준 일관화
- 예외 처리 및 공통 응답 구조 정리
- 추천 및 매칭 로직 정교화

## 팀 프로젝트 개요

본 프로젝트는 IT 취업 준비생을 주요 타깃으로 하여,
개인 기술 스택과 채용 시장 데이터를 연결하는 취업 전략 지원 플랫폼을 목표로 개발되었습니다.

