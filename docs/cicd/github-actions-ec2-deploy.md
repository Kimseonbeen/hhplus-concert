# CI/CD: GitHub Actions + Docker Hub + AWS EC2

## 전체 흐름

```
로컬 코드 Push
    ↓
GitHub Actions (CI/CD 파이프라인)
    ↓
[Job 1] Docker 이미지 빌드 → Docker Hub 푸시
    ↓
[Job 2] EC2 SSH 접속 → 최신 이미지 Pull → docker-compose 재시작
    ↓
EC2에서 앱 서비스 중
```

---

## 구성 요소

| 구성 요소 | 역할 |
|---|---|
| **GitHub Actions** | 코드 푸시 감지 → 빌드/배포 자동화 |
| **Docker Hub** | 빌드된 이미지 저장소 (레지스트리) |
| **AWS EC2** | 실제 서비스가 동작하는 서버 |
| **docker-compose** | EC2에서 컨테이너(앱, MySQL, Redis) 오케스트레이션 |

---

## 워크플로우 파일 설명 (`.github/workflows/deploy.yml`)

### 트리거
```yaml
on:
  push:
    branches: [ feat/dev ]
```
`feat/dev` 브랜치에 Push 시 자동 실행.

### Job 1: build-and-push
```yaml
- uses: actions/checkout@v3          # 코드 체크아웃
- uses: docker/login-action@v2       # Docker Hub 로그인
- uses: docker/build-push-action@v4  # Dockerfile로 빌드 후 Docker Hub에 푸시
```
- **Dockerfile 기반**으로 멀티스테이지 빌드 수행 (Gradle 빌드 → 실행 이미지)
- 빌드된 이미지를 `{DOCKERHUB_USERNAME}/hhplus-concert:latest` 태그로 Docker Hub에 업로드

### Job 2: deploy
```yaml
- uses: appleboy/ssh-action@master   # EC2 SSH 접속
  with:
    script: |
      cd ~/app
      git pull origin feat/dev          # docker-compose.yml 최신화
      docker pull ksb9242/hhplus-concert:latest  # 새 이미지 Pull
      docker-compose up -d              # 컨테이너 재시작
```
- EC2에 SSH로 접속해서 최신 이미지를 받고 컨테이너를 재시작
- `docker-compose up -d` 는 변경된 서비스만 재시작함

---

## Dockerfile 구조 (멀티스테이지 빌드)

```dockerfile
# 1단계: 빌드 스테이지
FROM eclipse-temurin:17-jdk-jammy AS builder
RUN apt-get update && apt-get install -y git
WORKDIR /app
COPY . .
RUN ./gradlew clean bootJar  # JAR 파일 생성

# 2단계: 실행 스테이지
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**멀티스테이지 빌드를 쓰는 이유:**
- 빌드 도구(Gradle, JDK full)는 최종 이미지에 포함할 필요 없음
- 실행 이미지에는 JAR 파일만 담아 이미지 크기 최소화

---

## GitHub Secrets 설정

GitHub 레포지토리 → Settings → Secrets and variables → Actions 에서 등록.

| Secret 키 | 값 |
|---|---|
| `DOCKERHUB_USERNAME` | Docker Hub 유저명 |
| `DOCKERHUB_TOKEN` | Docker Hub Access Token |
| `EC2_HOST` | EC2 퍼블릭 IP |
| `EC2_USER` | EC2 접속 유저 (ubuntu) |
| `EC2_KEY` | EC2 PEM 키 파일 내용 (전체) |
| `EC2_PORT` | SSH 포트 (보통 22) |

---

## EC2 초기 세팅 (최초 1회)

```bash
# Docker 설치
sudo apt-get update
sudo apt-get install -y docker.io
sudo usermod -aG docker ubuntu

# docker-compose v2 설치
sudo curl -L "https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
sudo ln -sf /usr/local/bin/docker-compose /usr/bin/docker-compose

# 앱 디렉토리 준비 (docker-compose.yml 위치)
mkdir ~/app
cd ~/app
git clone -b feat/dev {레포지토리 URL} .

# Swap 추가 (t2.micro RAM 1GB 부족 방지)
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
```

---

## 트러블슈팅 기록

### 1. `lstat /build/libs: no such file or directory`

**상황:** GitHub Actions에서 Docker 이미지 빌드 시 발생.

**원인:**
기존 Dockerfile이 로컬에서 미리 빌드된 JAR 파일을 복사하는 방식이었음.
```dockerfile
# 문제가 된 방식
COPY build/libs/*.jar app.jar
```
로컬에선 `./gradlew build` 후 `build/libs/`에 JAR가 존재하지만,
GitHub Actions의 Docker 빌드 컨텍스트에는 `.gitignore`로 인해 `build/` 폴더가 없음.

**해결:** 멀티스테이지 빌드로 변경 — Docker 내부에서 직접 Gradle 빌드 수행.
```dockerfile
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app
COPY . .
RUN ./gradlew clean bootJar

FROM eclipse-temurin:17-jdk-jammy
COPY --from=builder /app/build/libs/*.jar app.jar
```

---

### 2. `A problem occurred starting process 'command 'git''`

**상황:** 멀티스테이지 빌드로 변경 후 Gradle 빌드 단계에서 발생.

**원인:**
`eclipse-temurin:17-jdk-jammy` 베이스 이미지에는 git이 설치되어 있지 않음.
프로젝트의 `build.gradle`에 git 커밋 해시를 읽는 설정이 있어 빌드 시 git 명령어를 호출함.

**해결:** 빌드 스테이지에 git 설치 추가.
```dockerfile
FROM eclipse-temurin:17-jdk-jammy AS builder
RUN apt-get update && apt-get install -y git
```

---

### 3. `ssh: no key found` / `i/o timeout`

**상황:** deploy Job에서 EC2 SSH 접속 실패.

**원인 1 - `ssh: no key found`:**
`EC2_KEY` Secret에 PEM 파일 내용이 누락되거나 형식이 잘못됨.
PEM 파일은 아래처럼 헤더/푸터 포함 전체 내용을 붙여넣어야 함.
```
-----BEGIN RSA PRIVATE KEY-----
...
-----END RSA PRIVATE KEY-----
```

**원인 2 - `i/o timeout`:**
EC2 보안 그룹(Security Group) Inbound Rules에서 SSH(22번 포트)가 열려있지 않았음.

**해결:**
- EC2 콘솔 → 보안 그룹 → Inbound Rules → SSH(22) `0.0.0.0/0` 추가
- GitHub Secrets에 PEM 파일 전체 내용 재등록

---

### 4. `KeyError: 'ContainerConfig'`

**상황:** EC2에서 `docker-compose up -d` 실행 시 발생.

**원인:**
EC2에 설치된 docker-compose 버전이 `1.29.2`로 구버전이었음.
최신 Docker Engine과의 API 호환성 문제로 `ContainerConfig` 키를 파싱하지 못함.

**해결:** docker-compose를 v2.24.0으로 업그레이드.
```bash
sudo curl -L "https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-$(uname -s)-$(uname -m)" \
  -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
# /usr/bin/docker-compose(구버전)가 PATH 우선순위가 높아 심볼릭 링크로 덮어씀
sudo ln -sf /usr/local/bin/docker-compose /usr/bin/docker-compose
```

---

### 5. Redis `Permission denied` on appendonly.aof

**상황:** Redis 컨테이너가 `Exit 1`로 종료됨.

**원인:**
docker-compose에서 `./redis_data:/bitnami/redis/data` 볼륨을 마운트하는데,
`redis_data` 디렉토리의 소유권이 root여서 bitnami Redis 컨테이너(비root 유저)가 쓰지 못함.

**해결:**
```bash
sudo chmod -R 777 ~/app/redis_data
```

---

### 6. 앱 컨테이너 OOM으로 죽음 (Exit 255)

**상황:** 앱 컨테이너가 Spring Boot 시작 중 Repository 스캔 이후 로그 없이 종료됨.

**원인:**
- t2.micro는 RAM 1GB
- MySQL + Redis + InfluxDB/Grafana + Spring Boot JVM 동시 실행
- EC2 기본 설정에 Swap이 없음 (`Swap: 0`)
- JVM이 메모리 부족으로 OS에 의해 강제 종료(OOM Kill)됨
- exit code 255는 EC2 재부팅으로 컨테이너가 강제 종료된 경우에도 발생

**해결:** Swap 공간 추가.
```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
```
