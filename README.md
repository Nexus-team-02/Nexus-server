# Nexus Server
> 🔗 **서비스 링크**: [`https://nexus-team02.vercel.app`](https://nexus-team02.vercel.app)

Nexus는 복잡한 프로젝트의 흐름 속에서 흩어진 정보와 사람들을 하나로 연결하는 IT 협업 플랫폼입니다.</br>
다양한 분야의 팀원들이 각자의 관점에서 필요한 깊이만큼 정보를 확인할 수 있도록 구성되어 있으며, 기존의 시간 소모적이던 반복 작업을 자동화해 업무 효율을 높였습니다.</br>
개발자, 디자이너, 기획자 등 서로 다른 역할의 팀원들도 Nexus를 통해 하나의 흐름 속에서 함께 일할 수 있습니다.

## 기술 스택

| 영역 | 기술 |
|------|------|
| Runtime | Java 21, Spring Boot 3.5.7 |
| Database | MySQL 8.0, Spring Data JPA |
| Cache | Redis 6.0.9 |
| AI/RAG | Spring AI 1.1.2, OpenAI, Pinecone (Vector DB) |
| Security | Spring Security, JWT (jjwt 0.12.3) |
| Storage | AWS S3 (Spring Cloud AWS 3.1.1) |
| API Docs | SpringDoc OpenAPI 2.8.9 |
| Infra | Docker, Nginx 1.25, Let's Encrypt |

## 백엔드 팀원
|                                                         **김민지**                                                         |                                                      **강민서**                                                      |
|:-----------------------------------------------------------------------------------------------------------------------:|:-----------------------------------------------------------------------------------------------------------------:|
| <a href="https://github.com/minzix"><img src="https://avatars.githubusercontent.com/u/126869805?v=4" width="150"> | <a href="https://github.com/1224kang"><img src="https://avatars.githubusercontent.com/u/131092169?v=4" width="150"> |
|                                                        `backend`                                                        |                                                     `backend`                                                     |



## 아키텍처

```
Nexus-server/
├── src/main/java/pingpong/backend/
│   ├── domain/
│   │   ├── auth/          # 인증 (OAuth 2.0)
│   │   ├── chat/          # RAG 챗봇
│   │   ├── eval/          # LLM 평가 메트릭
│   │   ├── flow/          # 플로우/목업 관리
│   │   ├── member/        # 멤버 관리
│   │   ├── notion/        # Notion 연동
│   │   ├── qa/            # QA 테스트
│   │   ├── server/        # 서버/API 관리
│   │   ├── swagger/       # Swagger 스펙 관리
│   │   ├── task/          # Notion 태스크 동기화
│   │   └── team/          # 팀 관리
│   └── global/
│       ├── auth/          # Security 설정, JWT 필터
│       ├── config/        # 전역 설정
│       ├── exception/     # 예외 처리
│       ├── rag/           # RAG 파이프라인 (인덱싱/챗)
│       ├── redis/         # Redis 유틸
│       ├── response/      # 통합 응답 포맷
│       └── storage/       # AWS S3
├── mcp-server/            # MCP 서버 (별도 모듈)
├── nginx/conf.d/          # Nginx 설정
├── docker-compose.yml
└── Dockerfile
```
## ERD


## 시작하기

### 사전 요구사항

- Java 21+
- Docker & Docker Compose
- MySQL 8.0
- Redis 6.0+

### 환경 변수 설정

프로젝트 루트에 `.env` 파일을 생성합니다.

```env
# Database
MYSQL_DATABASE=pingpong
MYSQL_USER=your_db_user
MYSQL_PASSWORD=your_db_password
MYSQL_ROOT_PASSWORD=your_root_password

# JWT
JWT_SECRET=your_jwt_secret_key

# Redis
REDIS_PASSWORD=your_redis_password

# AWS
AWS_ACCESS_KEY_ID=your_aws_access_key
AWS_SECRET_ACCESS_KEY=your_aws_secret_key
AWS_S3_BUCKET=your_s3_bucket_name

# Application
PUBLIC_BASE_URL=https://your-domain.com

# Internal Dashboard (HTTP Basic Auth)
INTERNAL_USERNAME=your_internal_username
INTERNAL_PASSWORD=your_internal_password

# Notion OAuth
NOTION_CLIENT_ID=your_notion_client_id
NOTION_CLIENT_SECRET=your_notion_client_secret

# OpenAI
OPENAI_API_KEY=your_openai_api_key

# Pinecone
PINECONE_API_KEY=your_pinecone_api_key
```

### Docker로 실행

```bash
docker-compose up -d
```

서비스 구성:
- `server` - Spring Boot 앱 (포트 8080)
- `mysql` - MySQL 데이터베이스
- `redis` - Redis 캐시
- `nginx` - 리버스 프록시 (포트 80/443)
- `mcp-server` - MCP 서버 (포트 3000)

### 로컬 개발 실행

```bash
./gradlew bootRun
```

## RAG 파이프라인

Notion 문서를 기반으로 Pinecone에 벡터 임베딩을 저장하고, 사용자 질문에 관련 컨텍스트를 검색하여 OpenAI가 답변을 생성합니다.

```
Notion 페이지
    ↓ (텍스트 추출 + 청킹)
Pinecone (벡터 저장소)
    ↓ (유사도 검색)
컨텍스트 + 질문
    ↓ (OpenAI 생성)
스트리밍 응답 (SSE)
```

## LLM 평가 메트릭

`LlmEvalCase` 엔티티에서 다음 메트릭을 추적합니다:

| 카테고리 | 메트릭 |
|----------|--------|
| **검색 품질** | context_recall, context_precision |
| **생성 품질** | faithfulness_score, answer_relevance_score |
| **환각 탐지** | hallucination_rate, contradiction_flag |
| **운영 지표** | retrieval/generation/evaluation/total latency, tokens, cost |
| **유사도** | retrieved_doc_count, avg/min/max similarity |


