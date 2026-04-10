#  Workout Plan Architecture Refactor

## 1. Context & Goal
We are restructuring the workout builder in our Spring Boot + PostgreSQL backend. The previous architecture conflated a single "Workout" with a multi-week "Workout Plan." We are moving to a strict 4-level hierarchy: 
`Workout Plan (Macro) -> Plan Assignment (Link to Trainee) -> Workouts (Daily Sessions) -> Workout Exercises (Specific movements).`

**Note:** The database is currently empty/in development. We do NOT need to write data migration scripts for existing data. We are generating fresh entities and letting the schema update from scratch.

---

## 2. Database Schema & Entities

Please generate/refactor the JPA Entities (`@Entity`) for the following structure. Use `UUID` for all primary keys. Use Lombok annotations (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`) to keep boilerplate clean.

### Level 1: `WorkoutPlan`
*Replaces the old `exercises_plan` table.*
* `id` (UUID, Primary Key)
* `coachId` (UUID or Long, depending on current Users table - FK to Coaches)
* `title` (String, e.g., "30-Day Shred")
* `description` (String, nullable)
* `createdAt` (Timestamp)
* **Relationships:** `@OneToMany` with `Workout`, `@OneToMany` with `PlanAssignment`

### Level 2: `PlanAssignment`
*Replaces the old `exercise_plan_trainee` table.*
* `id` (UUID, Primary Key)
* `plan` (`@ManyToOne` mapping to `WorkoutPlan`)
* `traineeId` (UUID or Long - FK to Trainees)
* `startDate` (LocalDate)
* `status` (Enum: `ACTIVE`, `COMPLETED`, `DROPPED`)

### Level 3: `Workout`
*New table representing a single day/session.*
* `id` (UUID, Primary Key)
* `plan` (`@ManyToOne` mapping to `WorkoutPlan`)
* `title` (String, e.g., "Leg Day", "Full Body Circuit")
* `dayOrder` (Integer, e.g., 1, 2, 3 - used to order workouts within the plan)
* **Relationships:** `@OneToMany` with `WorkoutExercise` (Cascade = ALL, orphanRemoval = true)

### Level 4: `WorkoutExercise`
*Replaces the old `workout_exercise_items` table.*
* `id` (UUID, Primary Key)
* `workout` (`@ManyToOne` mapping to `Workout`)
* `exerciseId` (UUID or Long - FK to the master Exercises dictionary)
* `sectionType` (Enum: `WARM_UP`, `MAIN`, `COOL_DOWN`)
* `orderIndex` (Integer, to order exercises within their section)
* `sets` (Integer)
* `reps` (String or Integer)
* `loadAmount` (Double/BigDecimal)
* `restSeconds` (Integer)

---

## 3. Required API Flow & Controllers

We need REST endpoints that map to this hierarchical flow. Please generate the necessary Request/Response DTOs, Service layer methods, and Controller endpoints for the following actions:

**1. Create the Master Plan**
* `POST /api/v1/plans`
* **Payload:** `{ "title": "Hypertrophy Phase 1", "description": "...", "coachId": "..." }`
* **Returns:** Created `WorkoutPlan` DTO.

**2. Add Workouts (Days) to the Plan**
* `POST /api/v1/plans/{planId}/workouts`
* **Payload:** `{ "title": "Leg Day", "dayOrder": 1 }`
* **Returns:** Created `Workout` DTO.

**3. Populate the Exercises for a Specific Workout**
* `POST /api/v1/workouts/{workoutId}/exercises`
* **Payload:** List of exercise objects `[{ "exerciseId": "...", "sectionType": "WARM_UP", "orderIndex": 1, "sets": 3, "reps": "10", "loadAmount": 50.0, "restSeconds": 60 }]`
* **Returns:** List of created `WorkoutExercise` DTOs.

**4. Assign the Plan to a Trainee**
* `POST /api/v1/plans/{planId}/assign`
* **Payload:** `{ "traineeId": "...", "startDate": "2026-04-05" }`
* **Returns:** Created `PlanAssignment` DTO.

---

## 4. Cursor Execution Instructions (To be done sequentially)

1.  **Step 1:** Generate the updated JPA `@Entity` classes and the Enums (`SectionType`, `PlanStatus`). Stop and let me review.
2.  **Step 2:** Generate the Spring Data JPA Repositories for these entities.
3.  **Step 3:** Generate the Request and Response DTOs for the API endpoints.
4.  **Step 4:** Implement the `WorkoutPlanService` with the core business logic.
5.  **Step 5:** Create the `WorkoutPlanController` mapping the REST endpoints to the service.