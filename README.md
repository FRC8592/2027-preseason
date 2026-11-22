# 2027-preseason

Robot code for **FRC Team 8592**, carried onto the **WPILib 2027 alpha** toolchain as a preseason
migration project.

## What this is

This is the team's 2026 **REBUILT** competition codebase — swerve drive, turret + flywheel
shooter, indexer, intake, and three-camera AprilTag vision — ported onto WPILib 2027 alpha so the
software team can learn the new toolchain before the 2027 season starts. It was extracted from the
`robot-production/` subproject of [FRC8592/2026-Rebuilt](https://github.com/FRC8592/2026-Rebuilt).

Two things make this unlike most FRC repos, and both are worth knowing before you read any code:
the deploy target is **SystemCore** rather than a roboRIO, and WPILib 2027 renamed nearly every
package (`edu.wpi.first.*` is now `org.wpilib.*`). See [Migration notes](#migration-notes).

> [!NOTE]
> Migration in progress. Phoenix 6, REVLib, PathPlannerLib, AdvantageKit and PhotonLib have no
> 2027-alpha vendordep yet, so a fresh clone does not compile. That is expected — you did not
> break it. See [Migration notes](#migration-notes).

## Toolchain

| | |
| --- | --- |
| WPILib | `2027_alpha7` |
| GradleRIO | `org.wpilib.GradleRIO` `2027.0.0-alpha-8` |
| Gradle | `9.4.1` (wrapper) |
| Java | 25 |
| Deploy target | **SystemCore**, team 8592 |
| Vendordeps installed | `CommandsV2` 1.0.0 |

## Setup

1. Install the WPILib 2027 alpha release — it brings its own JDK and the VS Code extension.
2. Clone the repo.
3. Run `./gradlew` once from the repo root so the wrapper downloads its Gradle distribution.
4. Open the folder in VS Code (WPILib extension) or IntelliJ IDEA. IntelliJ users: the checked-in
   project config expects a **JDK 25** named `temurin-25`.
5. Run `./gradlew tasks` to see what you can do. Have fun.

Note that `includeDesktopSupport = false` in `build.gradle`, so desktop and JNI unit-test support
is limited.

Before your first pull request, read [`CONTRIBUTING.md`](CONTRIBUTING.md) — branch names, commit
subjects, what the PR template asks for, and what a reviewer checks.

## Gradle commands

```bash
./gradlew build                        # compile + test (this is what CI runs)
./gradlew test                         # JUnit 5 (no src/test exists yet)
./gradlew test --tests '*ClassName*'   # one test class
./gradlew deploy                       # deploy to SystemCore for team 8592
./gradlew simulateJava                 # simulation with GUI + driver station
./gradlew vendordep --url=<json url>   # install a vendor library
./gradlew Glass                        # also: SysId, OutlineViewer, DataLogTool, InstallAllTools
```

`simulateJava` is a hidden task — it works, but it will not appear in `./gradlew tasks` output.

## Simulation and log replay

`Robot` extends AdvantageKit's `LoggedRobot`. The simulation GUI and driver station are enabled in
`build.gradle`, so `./gradlew simulateJava` is all you need to drive the robot on your laptop.

Logging behavior depends on where the code is running:

- **On the robot** — WPILOG written to the USB stick, plus NT4 for live dashboards.
- **In simulation** — WPILOG written locally, plus NT4.
- **Replay** — set the `AKIT_LOG_PATH` environment variable to a `.wpilog` file and run
  `simulateJava`. `Robot` only enters replay mode when that variable is non-blank; otherwise
  simulation runs live.

Open the resulting logs in [AdvantageScope](https://docs.advantagescope.org). Custom log keys live
under `CustomLogs/<SUBSYSTEM>/` (see [Conventions](#conventions)), and the robot pose is published
to SmartDashboard as `Field`.

## Architecture

`Main` → `Robot` (`LoggedRobot`) → `RobotContainer`.

`Robot` does more than a template robot class: besides running the `CommandScheduler`, it
configures the AdvantageKit data receivers and drives a fair amount of logic from
`disabledPeriodic` (vision updates, LED tag count, alliance, shooter PID tuning from
SmartDashboard, match countdown timers). `RobotContainer` owns all construction and bindings.

- [`frc.robot`](src/main/java/frc/robot) — `Main`, `Robot`, `RobotContainer`, `Constants`,
  `BuildConstants`.
- [`frc.robot.subsystems`](src/main/java/frc/robot/subsystems) — `Scoring` is a **composite
  subsystem**: it constructs and publicly exposes `turret`, `shooter`, `indexer`, `intake` and
  `leds`, and its `periodic()` is the shooting brain — it picks a target by alliance and field
  region, solves the shoot-on-the-move flywheel RPM and field-relative turret angle, applies
  operator trim offsets, and selects a shooter PID slot by distance band. The leaf mechanisms are
  `Shooter`, `Turret`, `Indexer`, `Intake` and `LEDs`; `OdometryUpdates` filters vision poses into
  odometry. When you add mechanism behavior, decide deliberately whether it belongs in the leaf
  subsystem or in `Scoring`'s coordination.
- [`frc.robot.subsystems.swerve`](src/main/java/frc/robot/subsystems/swerve) — two layers.
  `CommandSwerveDrivetrain` extends CTRE's generated `TunerSwerveDrivetrain` (sim threading,
  alliance perspective, vision measurements, SysId); `TunerConstants` is Tuner X generated;
  `Swerve` is the team-facing wrapper on top (joystick shaping, field- vs robot-relative requests,
  heading lock and snap-to, X-brake) and is where PathPlanner's `AutoBuilder.configure(...)` is
  called. `Telemetry` publishes drivetrain state.
- [`frc.robot.subsystems.vision`](src/main/java/frc/robot/subsystems/vision) — `Vision` wraps
  PhotonLib, preferring multi-tag estimates and falling back to the lowest-ambiguity single tag.
  There is one `Vision` plus one `OdometryUpdates` per camera (back, right, left).
- [`frc.robot.commands.autonomous`](src/main/java/frc/robot/commands/autonomous) — `AutoManager`
  builds the `SendableChooser` from PathPlanner's `AutoBuilder.buildAutoChooser()` and adds
  hand-built options; `AutoCommands` holds named-command registrations.
- [`frc.robot.commands.proxies`](src/main/java/frc/robot/commands/proxies) — command wrappers with
  specific scheduler semantics: `MultiComposableCommand`, `OverrideEverythingCommand`,
  `TimingSimulatedCommand`, `WaitUntilCommand`. Prefer these over re-deriving the behavior.
- [`frc.robot.helpers`](src/main/java/frc/robot/helpers) — `TalonFXControl` and `SparkFlexControl`
  wrap Phoenix 6 and REVLib (config, PID slots, soft limits, current limits, logging);
  `SmoothingFilter` and `Utils` are math helpers.
- [`frc.robot.examples`](src/main/java/frc/robot/examples) — teaching reference code for learning
  TalonFX / SparkMax / SparkFlex patterns. Nothing on the robot instantiates it.

PathPlanner paths and autos live in
[`src/main/deploy/pathplanner/`](src/main/deploy/pathplanner) and are deployed as static files to
`/home/systemcore/deploy`.

## Controls

Two Xbox controllers: driver on port 0, operator on port 1. Bindings are declared as named
`Trigger` fields at the top of
[`RobotContainer`](src/main/java/frc/robot/RobotContainer.java) and wired in
`configureBindings()`.

### Driver

| Control | Action |
| --- | --- |
| Left stick / Right stick | Drive (translate / rotate) — the default command |
| `Back` | Reset heading |
| `Y` (hold) | Robot-relative drive |
| `A` | Snail mode (slow) |
| `D-pad ↑` | Less-slow mode |
| `X` | Lock wheels in an X to resist being pushed |
| `RB` (hold) | Heading lock — translate while holding heading |
| `LT` | Run intake rollers |
| `B` | Reverse intake rollers |
| `LB` | Extend intake |
| `RT` | Run indexer (shoot) |

### Operator

| Control | Action |
| --- | --- |
| `RT` | Toggle target tracking (turret + shooter wheels) |
| `A` | Manual override / turret lock |
| `RB` | Reverse indexer (unjam) |
| `LT` | Retract intake |
| `D-pad ↑` / `↓` | Trim shooter RPM up / down |
| `D-pad ←` / `→` | Trim turret angle left / right |

## Conventions

- **[`Constants.java`](src/main/java/frc/robot/Constants.java) is the single source** of CAN IDs,
  current limits, PID and feedforward gains, field geometry, camera transforms and LED colors,
  organized as nested `static final` classes named for each subsystem.
- **Log paths are constants.** Every subsystem defines
  `LOG_PATH = SHARED.LOG_FOLDER + "/<NAME>/"` and logs via
  `Logger.recordOutput(X.LOG_PATH + "key", value)`. Reuse that pattern instead of ad-hoc string
  literals.
- **Motor access goes through the helpers** — `TalonFXControl` and `SparkFlexControl` — rather than
  touching Phoenix 6 or REVLib directly. `Turret` is the deliberate exception: it needs dual
  `DutyCycleEncoder` absolute positioning and slot-level control.
- **New controls are named `Trigger` fields**, not inline `controller.x()` calls at the binding
  site.
- **Some values are live-tunable.** Several gains and timings are published to SmartDashboard and
  read back every loop, so changing a default in `Constants.java` is not always enough to change
  what the robot actually does.

### Git

Branches are named `<INITIALS>/<issue#>-<slug>` (for example
`BS/215-add-contributing-and-templates`), commit subjects are `#<issue#> - <summary>`, and pull
requests target `main` and need one approval from a software lead.

Issue numbers come from [FRC8592/2026-Rebuilt](https://github.com/FRC8592/2026-Rebuilt/issues) —
that is the team's tracker, and this repo has no issues of its own, so a PR here closes a ticket
only with the full form `Closes FRC8592/2026-Rebuilt#215`.

[`CONTRIBUTING.md`](CONTRIBUTING.md) is the source of truth for all of this, plus how pull requests
get reviewed and the ground rules for running code on a robot.

## Migration notes

WPILib 2027 renamed almost everything. **Assume any FRC example written for 2026 or earlier is
wrong about package names.**

| 2026 and earlier | 2027 |
| --- | --- |
| `edu.wpi.first.*` | `org.wpilib.*` |
| `edu.wpi.first.wpilibj2.command` | `org.wpilib.command2` (from the `CommandsV2` vendordep) |
| `DriverStation` | `org.wpilib.driverstation.{MatchState, RobotState, MatchType, Alliance, DriverStationErrors}` |
| `ChassisSpeeds` | `org.wpilib.math.kinematics.ChassisVelocities` (public `vx` / `vy` / `omega` fields) |
| `testInit` / `testPeriodic` / `testExit` | `utilityInit` / `utilityPeriodic` / `utilityExit` |
| `RobotBase` | `org.wpilib.framework.RobotBase` |
| roboRIO deploy target | SystemCore (`deploy { targets { systemcore ... } }`) |

The source imports five vendor libraries — Phoenix 6 (`com.ctre.phoenix6.*`), REVLib
(`com.revrobotics.*`), PathPlannerLib (`com.pathplanner.lib.*`), AdvantageKit
(`org.littletonrobotics.junction.*`) and PhotonLib (`org.photonvision.*`) — and none of them has a
2027-alpha vendordep in `vendordeps/` yet. Once a compatible build exists, install it with
`./gradlew vendordep --url=<json url>`. One in-WPILib move is also still unresolved:
`org.wpilib.vision.apriltag` (`AprilTagFieldLayout`, `AprilTagFields`) does not exist in alpha7.

## Licenses

- Team 8592 code in this repository is the team's own work.
- WPILib is used under the BSD-3-Clause license in [`WPILib-License.md`](WPILib-License.md).
- Vendor libraries (Phoenix 6, REVLib, PathPlannerLib, AdvantageKit, PhotonLib) are used under
  their respective licenses.
