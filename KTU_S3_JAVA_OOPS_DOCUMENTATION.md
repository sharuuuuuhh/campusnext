# CampusNest - KTU S3 Java Object-Oriented Programming (OOP) Project

**Course**: CST 205 - Object Oriented Programming Using Java  
**Institution**: College of Engineering Thalassery (COET) / APJ Abdul Kalam Technological University (KTU)  
**Semester**: S3 B.Tech CSE / IT  
**Project**: CampusNest - Student Paying Guest (PG) Accommodation Management System  

---

## 1. Executive Summary & OOP Architecture

CampusNest is built strictly using **Pure Java 17** following industry-standard Object-Oriented principles and a clean multi-tier enterprise architecture:

- **Presentation Tier**: Web Interface consuming Java Servlets
- **Controller Tier**: [ApiServlet](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/ApiServlet.java) handling HTTP sessions, CSRF, and routing
- **Service Tier**: Business logic, security, and rule enforcement ([AuthService](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/service/AuthService.java), [ListingService](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/service/ListingService.java), [InquiryService](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/service/InquiryService.java), [FeedbackService](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/service/FeedbackService.java), [AdminService](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/service/AdminService.java))
- **DAO (Data Access Object) Tier**: Database abstraction over JDBC ([UserDao](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/dao/UserDao.java), [OwnerDao](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/dao/OwnerDao.java), [ListingDao](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/dao/ListingDao.java), [InquiryDao](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/dao/InquiryDao.java), [FeedbackDao](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/dao/FeedbackDao.java))
- **Multithreading Worker**: Background daemon thread ([BackgroundSyncDaemon](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/thread/BackgroundSyncDaemon.java))
- **Database Tier**: MySQL 8.x with relational integrity

---

## 2. KTU Syllabus OOP Concepts Mapping (CST 205)

### Module 1: Classes, Objects, and Encapsulation
- **Encapsulation**:
  - All domain model attributes are declared `private` or `protected` with public accessors and mutators.
  - Invariant validation occurs directly inside setters, constructors, and explicit `validate()` methods.
  - Example: [PgListing.java](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/model/PgListing.java) ensures `vacantBeds <= totalBeds` and `monthlyRent > 0`.
  - Example: [User.java](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/model/User.java) enforces RFC 5322 regex validation on email addresses.

### Module 2: Inheritance, Polymorphism, Abstraction & Interfaces

#### A. Inheritance Hierarchy
```
           +--------------------+
           |    BaseEntity      | (Abstract Base Class)
           +--------------------+
             ^        ^       ^
             |        |       +------------------------------------+
             |        +---------------------+                      |
      +---------------+      +--------------+-----+       +--------+-------+
      |     User      |      |     PgListing      |       |    Inquiry     |
      |  (Abstract)   |      +--------------------+       |   Feedback     |
      +---------------+                                   +----------------+
        ^     ^     ^
        |     |     +-------------------------+
        |     +------------------+            |
  +-----+--------+        +------+-----+   +--+----------+
  | StudentUser  |        | OwnerUser  |   |  AdminUser  |
  +--------------+        +------------+   +-------------+
```
- **Base Class**: [BaseEntity](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/model/BaseEntity.java) implements [Identifiable](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/model/Identifiable.java), [Auditable](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/model/Auditable.java), and [Validatable](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/model/Validatable.java).
- **Subclasses**:
  - `User extends BaseEntity`
  - `StudentUser extends User`
  - `OwnerUser extends User`
  - `AdminUser extends User`
  - `PgListing extends BaseEntity`
  - `Inquiry extends BaseEntity`
  - `Feedback extends BaseEntity`

#### B. Polymorphism
1. **Compile-time Polymorphism (Method Overloading)**:
   - In [ListingDao.java](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/dao/ListingDao.java):
     - `findListings(ListingFilterCriteria criteria)` - filters by query, rent, room type, distance.
     - `findListings(long ownerId)` - retrieves listings owned by a specific owner.
   - In [ListingFilterCriteria.java](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/model/ListingFilterCriteria.java):
     - Multiple constructors for default, query-only, and comprehensive search parameters.
2. **Runtime Polymorphism (Method Overriding & Dynamic Binding)**:
   - `User.getDashboardRole()`:
     - `StudentUser` returns `"STUDENT"`
     - `OwnerUser` returns `"OWNER"`
     - `AdminUser` returns `"ADMIN"`
   - `User.hasAdminPrivileges()`:
     - Returns `false` for students and owners, and `true` for administrators.
   - `User.toSessionMap()`:
     - Dynamically formats session payload based on the concrete instance type.
   - `Validatable.validate()`:
     - Overridden in `StudentUser`, `OwnerUser`, `PgListing`, `Inquiry`, and `Feedback` with model-specific domain validation rules.

#### C. Abstraction & Interfaces
- Generic CRUD Contract: [GenericDao<T, ID>](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/dao/GenericDao.java)
- Domain DAOs: [UserDao](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/dao/UserDao.java), [OwnerDao](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/dao/OwnerDao.java), [ListingDao](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/dao/ListingDao.java), [InquiryDao](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/dao/InquiryDao.java), [FeedbackDao](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/dao/FeedbackDao.java)
- Service Contracts: [AuthService](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/service/AuthService.java), [ListingService](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/service/ListingService.java), [InquiryService](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/service/InquiryService.java), [FeedbackService](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/service/FeedbackService.java), [AdminService](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/service/AdminService.java)

---

### Module 3: Exception Handling & Multithreading

#### A. Custom User-Defined Exception Hierarchy
```
java.lang.Throwable
  ^
  |
java.lang.Exception
  ^
  |
CampusNestException (Base Custom Exception, HTTP status code mapping)
  ^
  +-- ValidationException (400 Bad Request)
  +-- AuthenticationException (401 Unauthorized)
  +-- UnauthorizedAccessException (403 Forbidden)
  +-- ResourceNotFoundException (404 Not Found)
  +-- DuplicateResourceException (409 Conflict)
  +-- DatabaseOperationException (500 Internal Server Error, wraps SQLException)
```
- Custom exceptions reside in `edu.coet.campusnest.exception`.
- Caught uniformly in [ApiServlet.java](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/ApiServlet.java) to return standardized JSON error messages to clients.

#### B. Multithreading
- **Worker**: [BackgroundSyncDaemon.java](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/thread/BackgroundSyncDaemon.java) implements `java.lang.Runnable`.
- **Lifecycle**: Managed by [CampusNestListener.java](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/CampusNestListener.java) via `contextInitialized` and `contextDestroyed`.
- **Synchronization**: Thread-safe synchronized method `logSyncSummary()` protects shared state across concurrent execution cycles.
- **Interruption**: Uses `Thread.interrupt()` and `Thread.join()` for graceful shutdown.

---

### Module 4: Collections Framework & JDBC

- **Collections**:
  - `List<PgListing>`, `List<Inquiry>`, `List<Feedback>` with `ArrayList`
  - `Map<String, Object>` with `LinkedHashMap` to maintain key ordering
  - `Set<String>` for allowed MIME types
  - Java Streams: `listings.stream().map(l -> l.toMap(false)).toList()`
- **JDBC Implementation**:
  - Uses [Db.java](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/Db.java) to acquire MySQL connections with connection pooling support.
  - Safe SQL execution via `PreparedStatement` to defend against SQL Injection.
  - Auto-generated key retrieval via `Statement.RETURN_GENERATED_KEYS`.
  - Transaction management with `setAutoCommit(false)` and `commit()/rollback()` in [AuthServiceImpl.java](file:///c:/Users/sharu/campusnext/src/main/java/edu/coet/campusnest/service/impl/AuthServiceImpl.java).

---

## 3. Package Structure

```
edu.coet.campusnest
├── ApiServlet.java                  # Main Controller Servlet
├── CampusNestListener.java          # Lifecycle WebListener & Thread Manager
├── Db.java                          # JDBC Connection Factory
├── Json.java                        # Jackson JSON Helper
├── Passwords.java                   # PBKDF2 Password Hashing
├── dao                              # Data Access Layer Interfaces
│   ├── GenericDao.java
│   ├── UserDao.java
│   ├── OwnerDao.java
│   ├── ListingDao.java
│   ├── InquiryDao.java
│   ├── FeedbackDao.java
│   └── impl                         # JDBC Implementations
│       ├── AbstractJdbcDao.java
│       ├── UserDaoImpl.java
│       ├── OwnerDaoImpl.java
│       ├── ListingDaoImpl.java
│       ├── InquiryDaoImpl.java
│       └── FeedbackDaoImpl.java
├── exception                        # Custom Exception Hierarchy
│   ├── CampusNestException.java
│   ├── AuthenticationException.java
│   ├── DatabaseOperationException.java
│   ├── DuplicateResourceException.java
│   ├── ResourceNotFoundException.java
│   ├── UnauthorizedAccessException.java
│   └── ValidationException.java
├── model                            # Domain Models & Interfaces
│   ├── Identifiable.java
│   ├── Auditable.java
│   ├── Validatable.java
│   ├── BaseEntity.java
│   ├── User.java
│   ├── StudentUser.java
│   ├── OwnerUser.java
│   ├── AdminUser.java
│   ├── PgListing.java
│   ├── Inquiry.java
│   ├── Feedback.java
│   ├── ListingFilterCriteria.java
│   └── enums                        # Domain Enums
│       ├── AccountStatus.java
│       ├── FeedbackStatus.java
│       ├── GenderType.java
│       ├── InquiryStatus.java
│       ├── ModerationStatus.java
│       ├── OwnerStatus.java
│       ├── RoomType.java
│       └── UserRole.java
├── service                          # Business Logic Service Interfaces
│   ├── AuthService.java
│   ├── ListingService.java
│   ├── InquiryService.java
│   ├── FeedbackService.java
│   ├── AdminService.java
│   └── impl                         # Service Implementations
│       ├── AuthServiceImpl.java
│       ├── ListingServiceImpl.java
│       ├── InquiryServiceImpl.java
│       ├── FeedbackServiceImpl.java
│       └── AdminServiceImpl.java
└── thread                           # Multithreaded Background Workers
    └── BackgroundSyncDaemon.java
```

---

## 4. How to Compile and Run

### 1. Build WAR Package with Maven
```powershell
mvn clean compile package
```

### 2. Run Local Development Server
```powershell
./start-local.ps1
```
Access the application in your browser at `http://127.0.0.1:8081/campusnest/`.

---

## 5. KTU Viva Voce Preparation Guide (Questions & Answers)

**Q1: How is Inheritance used in this project?**  
*Answer*: We implemented an inheritance tree rooted at `BaseEntity` (providing `id`, `createdAt`, `updatedAt`, and `validate()`). `User` inherits from `BaseEntity` as an abstract class, and is specialized into `StudentUser`, `OwnerUser`, and `AdminUser`.

**Q2: What is the difference between Method Overloading and Method Overriding here?**  
*Answer*:
- **Method Overloading**: In `ListingDao`, `findListings(ListingFilterCriteria criteria)` and `findListings(long ownerId)` share the same method name but accept different parameters.
- **Method Overriding**: `StudentUser`, `OwnerUser`, and `AdminUser` all override `User.getDashboardRole()` and `User.hasAdminPrivileges()` with their specific behavior.

**Q3: Where is Multithreading used?**  
*Answer*: The class `BackgroundSyncDaemon` implements `Runnable` and is spawned as a daemon thread in `CampusNestListener`. It runs periodic health checks and status heartbeats concurrently with web user requests, using the `synchronized` keyword to protect shared log/cycle variables.

**Q4: How did you implement user-defined exceptions?**  
*Answer*: We created `CampusNestException` extending `java.lang.Exception`. Specific subclasses such as `ValidationException`, `AuthenticationException`, `DuplicateResourceException`, and `ResourceNotFoundException` carry appropriate HTTP status codes and domain error messages.

**Q5: How does JDBC prevent SQL injection?**  
*Answer*: All database queries in our DAO layer use `java.sql.PreparedStatement` with positional `?` placeholders instead of concatenating strings.
