# 첫 번째 스테이지 -> build 영역(첫 번쨰 스테이지 자체를 빌드라는 이름 지정)
# 베이스 이미지로 amazoncorretto:17 버전을 사용하겠다.
# 이미지 빌드 시 java 17 버전이 설치된 리눅스 환경을 설치해라.
#JDK 자바에 관련된 모든 것이 전부 내장되어있는 개발 도구
FROM eclipse-temurin:17-jdk-alpine AS build

# 작업 폴더 지정 (이제부터 컨테이너 안의 /app 이라는 폴더에서 작업 선언)
WORKDIR /app

# 지금 이 Dockerfile을 기준으로 현재 경로에 있는 모든 파일 (소스코드, gradlew, ...)
# 모든 파일을 작업폴더인 /app 으로 복사
COPY . .

# chmod를 통해 Gradle wrapper 파일을 실행할 수 있는 권한을 주어야 한다.
# 컨테이너 기준에서는 gradlew가 외부 파일이기 때문에 실행할 수 있는 권한이 없다.
RUN chmod +x ./gradlew

# gradlew에게 기존에 있던 건 지우고(clean) 새롭게 빌드(build)
# 개발 과정에서는 시간 단축 등을 위해 test를 생략하고 빌드하는 것도 가능. (-x)
RUN ./gradlew clean build -x test


# 도커 이미지는 jar 파일만 있으면 되기 때문에 스테이지를 나누지 않기 때문에 용량이 큼, 서로 다른 단계
# 여기는 두 번째 스테이지 -> 실행 영역
# JRE , JAVA RUNTIME ENUIRONMENT 자바 를 실행할 수 있게 환경만 구성해주는 도구
FROM eclipse-temurin:17-jre-alpine

# build라는 별칭으로 만들어 진 첫번 째 스테이지에서
# .jar로 끝나는 파일을 app.jar로 복사해서 이미지 세팅
COPY --from=build /app/build/libs/*.jar app.jar

# 타임존 설정,
# 설치할 때 임시 저장소 저장하지 말고 날려보리고 TZ 데이터라는 값을 가져와 아시아 서울 맞춰놓겠다
# 백엔드 KST 설정을 안하면 그런 혼동이 있음, DB에서도 시간대를 아시아 서울 맞추는 것을 권장
ENV TZ=Asia/Seoul
RUN apk add --no-cache curl tzdata

# 이 컨테이너가 시작될 때 무조건 실행해야 하는 명령어
# CMD는 기본 실행 명령어를 의미. 컨테이너 실행 시에 다른 명령어가 주어지면 그 명령어로 대체됨.
# ENTRYPOINT는 반드시 실행되어야 할 명령어를 의미. 다른 명령어로 대체되지 않음.
# 스프링 부트는 무조건 -jar 옵션으로 실행되어야 하기에 ENTRYPOINT로 안전하게 선언.
ENTRYPOINT ["java", "-jar", "app.jar"]