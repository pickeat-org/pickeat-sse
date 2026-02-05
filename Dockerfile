# 빌드용 jdk AS 키워드를 사용하여 해당 스테이지 이름을 build로 설정
FROM eclipse-temurin:21 as build

WORKDIR /app

# Gradle 관련 파일 복사
COPY build.gradle .
COPY settings.gradle .
COPY gradlew .
COPY gradle ./gradle

# 의존성 다운로드
RUN ./gradlew clean --no-daemon || true

# 소스 코드 복사
COPY src ./src

# 빌드 (테스트 제외)
RUN ./gradlew bootJar -x test --no-daemon

# 실행용 jre
FROM eclipse-temurin:21-jre

WORKDIR /app

# 빌드 결과물 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 서브모듈 설정 파일 복사 (추가!)
COPY --from=build /app/src/main/resources/config /app/config

EXPOSE 8080

# 실행 (실시간 로그 확인 가능)
# config 디렉토리를 spring.config.additional-location으로 지정
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-} -Dspring.profiles.active=${SPRING_ACTIVE_PROFILE:-default} -Dspring.config.additional-location=file:/app/config/ -jar /app/app.jar"]

