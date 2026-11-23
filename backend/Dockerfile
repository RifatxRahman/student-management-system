# ==============================================================================
# Phase 1: Build Stage (Maven + Eclipse Temurin JDK 21 Alpine)
# ==============================================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# ক্যাশিং সুবিধার জন্য প্রথমে শুধু pom.xml কপি
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# সোর্স কোড কপি ও জার প্যাকেজিং
COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true -B

# ==============================================================================
# Phase 2: Production Runtime Stage (Eclipse Temurin JRE 21 Alpine)
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine

# সিকিউরিটি হার্ডেনিং: নন-রুট ইউজার তৈরি
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# বিল্ডার পর্যায় থেকে জার কপি
COPY --from=builder /app/target/*.jar app.jar
RUN chown -R spring:spring /app

USER spring:spring

EXPOSE 8080

# হেলথচেক প্রব
HEALTHCHECK --interval=20s --timeout=5s --start-period=60s --retries=5 \
  CMD wget -q -O - http://127.0.0.1:8080/api/ping > /dev/null || exit 1

# প্রোডাকশন জেভিএম টিউনিং ফ্ল্যাগসহ এন্ট্রি পয়েন্ট
ENTRYPOINT ["java", \
  "-XX:+UseG1GC", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:InitialRAMPercentage=50.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=prod", \
  "-jar", "app.jar"]
