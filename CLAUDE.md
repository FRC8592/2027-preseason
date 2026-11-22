# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

FRC Team 8592 (`github.com/FRC8592/2027-preseason`) robot code. It is the team's 2026 "REBUILT" competition
codebase (turret + flywheel shooter, indexer, intake, swerve, 3-camera AprilTag vision) carried into the
**WPILib 2027 alpha** toolchain as a preseason migration project. There is no README; this file and the code
are the documentation.

Key platform facts that make this unlike most FRC repos:

- **WPILib `2027_alpha7`** (`.wpilib/wpilib_preferences.json`), GradleRIO `2027.0.0-alpha-8`, **Java 25**.
- Deploy target is **SystemCore**, not roboRIO (`deploy { targets { systemcore ... } }`).
- 2027 renamed almost every WPILib package: `edu.wpi.first.*` → `org.wpilib.*`. Commands are
  `org.wpilib.command2` (from the `CommandsV2` vendordep), `DriverStation` split into
  `org.wpilib.driverstation.{MatchState, RobotState, MatchType, Alliance, DriverStationErrors}`,
  `ChassisSpeeds` → `org.wpilib.math.kinematics.ChassisVelocities` (public `vx`/`vy`/`omega` fields),
  and `TimedRobot`'s `testInit/testPeriodic/testExit` are now `utilityInit/utilityPeriodic/utilityExit`.
  When migrating code or looking up APIs, assume 2026-and-earlier examples are wrong about package names.

## Build state: currently broken on purpose-in-progress

`./gradlew build` fails with ~2800 compile errors. The cause is that `vendordeps/` contains **only**
`CommandsV2.json`, while the source imports five vendor libraries that have no vendordep installed:

| Missing package | Library |
| --- | --- |
| `com.ctre.phoenix6.*` | CTRE Phoenix 6 (swerve, TalonFX, CANcoder, Pigeon2, CANdle) |
| `com.revrobotics.*` | REVLib (SparkFlex/SparkMax) |
| `com.pathplanner.lib.*` | PathPlannerLib |
| `org.littletonrobotics.junction.*` | AdvantageKit |
| `org.photonvision.*` | PhotonLib |

Plus one in-WPILib move to resolve: `org.wpilib.vision.apriltag` (`AprilTagFieldLayout`, `AprilTagFields`)
does not exist in alpha7. There are also a few genuine source bugs behind the vendordep wall (e.g.
`Indexer.indexerJamming` is referenced by `Indexer` and `Scoring` but never declared).

Install vendordeps with `./gradlew vendordep --url=<json url>` (or `--url=` a file in the local WPILib
folder); 2027-alpha-compatible builds of these vendor libraries may not exist yet, which is the real
blocker. Don't assume a compile failure means you broke something — check whether it is one of the above.
`build/reports/problems/problems-report.html` holds the last build's full error list.

## Commands

```bash
./gradlew build                      # compile + test (what CI runs)
./gradlew test                       # JUnit 5; no src/test exists yet
./gradlew test --tests '*ClassName*' # single test class
./gradlew deploy                     # deploy to SystemCore for team 8592
./gradlew simulateJava               # sim with GUI + driver station (hidden task; not in `tasks` output)
./gradlew vendordep --url=<url>      # install a vendor library JSON
./gradlew Glass | SysId | OutlineViewer | DataLogTool | InstallAllTools
```

AdvantageKit log replay: set `AKIT_LOG_PATH` to a `.wpilog` and run `simulateJava`. Without that env var,
sim runs live (NT4 + local log); `Robot` only enters replay mode when `AKIT_LOG_PATH` is non-blank.

Note `includeDesktopSupport = false` in `build.gradle`, so desktop/unit-test JNI is limited.

**CI is broken independently of the above**: `.github/workflows/gradle.yml` sets
`working-directory: robot-production`, a directory that does not exist in this repo (leftover from the
project this code was imported from — see also the stale checked-in `BuildConstants.java` with
`MAVEN_NAME = "robot-production"`). It also runs the `roborio-cross-ubuntu:2025` container, which predates
SystemCore and JDK 25.

## Architecture

`Main` → `Robot` (`LoggedRobot`, AdvantageKit) → `RobotContainer`.

**`Robot`** does more than a template robot class. Besides running the `CommandScheduler`, it configures
AdvantageKit receivers (real: WPILOG to USB + NT4; sim: WPILOG + NT4, or replay), and its
`disabledPeriodic` manually drives a lot of logic: it calls `Vision.periodic()` on all three cameras,
counts visible tags to drive the LED strip, pushes the DriverStation alliance into `Scoring`, and calls
`shooter.updatePID()` so PID values can be tuned from SmartDashboard while disabled. Match countdown
timers (`AutoTime`, `TelopTime`) are also published here. Beware: `Vision` extends `SubsystemBase`, so the
scheduler already calls its `periodic()`; the manual calls in `disabledPeriodic` double-invoke
`camera.getAllUnreadResults()`, which `Vision` itself documents as "MUST ONLY CALL ONCE PER LOOP".

**`RobotContainer`** owns construction and all bindings. Controller triggers are declared as named
`Trigger` fields (`INTAKE_RUN`, `ENABLE_TRACKING`, `SHOOT`, …) at the top of the class and wired in
`configureBindings()` — add new controls as a named field, not an inline `controller.x()` call.
`configureDefaults()` goes through a private `setDefaultCommand` guard that rejects any default command
whose interruption behavior is `kCancelIncoming`.

**`Scoring` is a composite subsystem**, not a leaf: it constructs and publicly exposes `turret`, `shooter`,
`indexer`, `intake`, plus `leds`, and `RobotContainer.scoring` is public so `Robot` reaches through the
chain (`m_robotContainer.scoring.intake.stopRoller()`). Its `periodic()` is the shooting brain: picks the
target pose by alliance + field region (`getTarget`), computes shoot-on-the-move flywheel RPM and
field-relative turret angle (`SOTM`, which numerically solves a projectile equation via scan + bisection in
`solveV0y`/`bisect`), applies operator trim offsets (`shooterSpeedOffset`, `turretAngleOffset`), and picks a
shooter PID slot (0/1/2) by distance band (≤2 m, 2–4 m, >4 m). Tracking is a toggle
(`toggleTrackingCommand`), with `overrideTracking` as a manual fallback that holds the turret and runs a
fixed RPM. When adding mechanism behavior, decide deliberately whether it belongs in the leaf subsystem or
in `Scoring`'s coordination.

**Swerve is two layers.** `CommandSwerveDrivetrain` extends CTRE's generated `TunerSwerveDrivetrain` and
handles sim threading, alliance perspective, vision measurements, and SysId routines; `TunerConstants` is
Tuner X generated (hardware IDs, gearing, gains pulled from `Constants.SWERVE`). `Swerve` is the
team-facing `SubsystemBase` wrapper on top: joystick shaping (`processJoystickInputs` — exponent curve +
`SmoothingFilter` moving average + snail/less-slow speed scaling), field- vs robot-relative requests,
heading lock/snap-to PID, X-brake, and the **PathPlanner `AutoBuilder.configure(...)` call lives in its
constructor**. If `RobotConfig.fromGUISettings()` throws, the constructor `return`s early and AutoBuilder is
never configured — autos then fail silently.

**Vision → odometry is one `Vision` + one `OdometryUpdates` per camera** (back/right/left), constructed in
`RobotContainer`. `Vision` wraps PhotonLib (multi-tag first, lowest-ambiguity fallback) and optionally
PhotonVision sim via its second constructor. `OdometryUpdates` is the filter: while disabled it hard-sets
odometry to the vision pose; while enabled it accepts multi-tag poses unconditionally and single-tag poses
only under `VISION.MAX_ACCEPTABLE_AMBIGUITY` and inside `REJECT_SINGLE_TAG_POSE_ESTIMATE_RANGE`.

**Autonomous.** PathPlanner paths/autos live in `src/main/deploy/pathplanner/` (deployed as static files).
`AutoManager.prepare()` builds the `SendableChooser` via `AutoBuilder.buildAutoChooser()`, then adds
hand-built options (mirrored paths, a two-swipe sequence) inside try/catch blocks that report to the driver
station on failure. `getAutonomousCommand()` prepends a dashboard-configurable `Auto Delay` wait.
Two gotchas: `NamedCommands`/`EventTrigger` registration is **duplicated** — the live copy is inline in
`RobotContainer`'s constructor, while `AutoCommands.registerAll()` is a near-identical unused copy
(`// AutoCommands.registerAll(scoring)` is commented out), so edit the `RobotContainer` copy; and
`AutoManager` loads a path named `"HALF LEFT"` that does not exist under `deploy/pathplanner/paths`, so
those mirrored options always fall into their catch blocks.

### Conventions

- **`Constants.java` is the single source** of CAN IDs, current limits, PID/FF gains, field geometry, camera
  transforms, and LED colors, organized as nested `static final` classes named for the subsystem.
- **Log paths** are constants: every subsystem has `LOG_PATH = SHARED.LOG_FOLDER + "/<NAME>/"` and logs via
  `Logger.recordOutput(X.LOG_PATH + "key", value)`. Reuse that pattern rather than ad-hoc string literals.
- **Motor access goes through `helpers/TalonFXControl` and `helpers/SparkFlexControl`** — thin wrappers over
  Phoenix 6 / REVLib that bundle config, PID slots, soft limits, current limits, and `motorLogging()`.
  `Turret` is the exception and talks to `TalonFX` directly (it needs dual `DutyCycleEncoder` CRT absolute
  positioning and slot-level control).
- `frc/robot/examples/` is **teaching reference code** for team members (TalonFX / SparkMax / SparkFlex
  patterns); nothing on the robot instantiates it. Don't wire it into the robot, and don't "fix" it to match
  production classes.
- `commands/proxies/` holds custom command wrappers with specific scheduler semantics —
  `MultiComposableCommand` (run a command without registering it as composed),
  `OverrideEverythingCommand` (cancel all, then schedule), `TimingSimulatedCommand` (replace with a wait in
  sim), `WaitUntilCommand`. Prefer these over re-deriving the behavior.
- Tunable values are published to SmartDashboard and read back each loop (`shooterV`, `SOTMdt`,
  `Wait Command Auto`, `Auto Delay`, PID keys via `updatePID()`), so changing a `Constants` default is not
  always sufficient to change runtime behavior.

### Git conventions

Branches are named `<INITIALS>/<issue#>-<slug>` (e.g. `BS/215-add-contributing-and-templates`) and commit
subjects are `#<issue#> - <summary>`. PRs target `main` and need one approval from a software lead.
`CONTRIBUTING.md` is the source of truth for all of this.

**Issue numbers come from `FRC8592/2026-Rebuilt`**, not this repo — that is the team's tracker, and
this repo has no issues of its own. A bare `#215` therefore does not close anything from a PR here;
the closing keyword needs the full `Closes FRC8592/2026-Rebuilt#215` form.
