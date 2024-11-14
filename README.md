# Directory Structure
---
~~~
lsm/
├── .gradle/
├── build/
├── gradle/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── lsm/
│   │   │           ├── LsmApplication.java
│   │   │           ├── aspect/
│   │   │           │   ├── LoggingAspect.java
│   │   │           ├── config/
│   │   │           │   ├── AspectConfig.java
│   │   │           │   ├── CacheConfig.java
│   │   │           │   ├── RedisConfig.java
│   │   │           │   ├── SecurityConfig.java
│   │   │           │   ├── SwaggerConfig.java
│   │   │           ├── controller/
│   │   │           │   ├── ApiResponse_.java
│   │   │           │   ├── AssignmentController.java
│   │   │           │   ├── AttendanceController.java
│   │   │           │   ├── AuthController.java
│   │   │           │   ├── ContentController.java
│   │   │           ├── events/
│   │   │           │   ├── EventPublisher.java
│   │   │           │   ├── UserEvent.java
│   │   │           │   ├── UserLoginEvent.java
│   │   │           │   ├── UserLogoutEvent.java
│   │   │           │   ├── UserRegisteredEvent.java
│   │   │           ├── exception/
│   │   │           │   ├── AuthenticationException.java
│   │   │           │   ├── DuplicateResourceException.java
│   │   │           │   ├── InvalidTokenException.java
│   │   │           │   ├── AccountDisabledException.java
│   │   │           │   ├── AccountLockedException.java
│   │   │           │   ├── InvalidPasswordException.java
│   │   │           │   ├── LogoutException.java
│   │   │           │   ├── RateLimitExceededException.java
│   │   │           │   ├── TokenExpiredException.java
│   │   │           │   ├── TokenValidationException.java
│   │   │           │   ├── UserNotFoundException.java
│   │   │           ├── mapper/
│   │   │           │   ├── UserMapper.java
│   │   │           ├── model/
│   │   │               ├── DTOs/
│   │   │               │   ├── AssignmentDTO.java
│   │   │               │   ├── AssignmentRequestDTO.java
│   │   │               │   ├── AttendanceDTO.java
│   │   │               │   ├── AttendanceRequestDTO.java
│   │   │               │   ├── AttendanceStatsDTO.java
│   │   │               │   ├── AuthenticationResult.java
│   │   │               │   ├── LoginRequestDTO.java
│   │   │               │   ├── LoginResponseDTO.java
│   │   │               │   ├── RegisterRequestDTO.java
│   │   │               │   ├── RegisterResponseDTO.java
│   │   │               │   ├── TokenRefreshResponseDTO.java
│   │   │               │   ├── TokenRefreshResult.java
│   │   │               │   ├── UserDTO.java
│   │   │               ├── entity/
│   │   │                   ├── base/
│   │   │                   │   ├── AppUser.java
│   │   │                   ├── enums/
│   │   │                   │   ├── AssignmentStatus.java
│   │   │                   │   ├── AttendanceStatus.java
│   │   │                   │   ├── Role.java
│   │   │                   ├── Assignment.java
│   │   │                   ├── Attendance.java
│   │   │                   ├── ClassEntity.java
│   │   │                   ├── RefreshToken.java
│   │   │                   ├── StudentDetails.java
│   │   │               ├── validation/
│   │   │                   ├── contraint/
│   │   │                   │   ├── TCConstraint.java
│   │   │                   │   ├── PasswordConstraint.java
│   │   │                   ├── groups/
│   │   │                   │   ├── ValidationGroups.java
│   │   │                   ├── LoginRequestValidator.java
│   │   │                   ├── PasswordConstraintValidator.java
│   │   │                   ├── TCValidator.java
│   │   │            ├── repository/
│   │   │               ├── AppUserRepository.java
│   │   │               ├── AssignmentRepository.java
│   │   │               ├── AttendanceRepository.java
│   │   │               ├── RefreshTokenRepository.java
│   │   │            ├── security/
│   │   │               ├── JwtAuthenticationFilter.java
│   │   │               ├── RateLimiter.java
│   │   │               ├── RateLimitProperties.java
│   │   │            ├── service/
│   │   │               ├── AppUserService.java
│   │   │               ├── AssignmentService.java
│   │   │               ├── AttendanceService.java
│   │   │               ├── AuthService.java
│   │   │               ├── JwtTokenProvider.java
│   │   │               ├── LoginAttemptsService.java
│   │   │       └── resources/
│   │   │           ├── static/
│   │   │               ├── css/
│   │   │               │   ├── ...
│   │   │               ├── js/
│   │   │                   ├── ...
│   │   │           ├── templates/
│   │   │           │   ├── ...
│   │   │           ├── application.properties
│   ├── test/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── lsm/...
├── .env
├── .gitignore
├── Dockerfile
├── docker-compose.yml
├── build.gradle
├── gradlew
├── lsm_database_dump.sql
├── settings.gradle
