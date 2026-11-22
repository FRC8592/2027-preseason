## Issue

<!-- The tracker is a different repo, so the full form is required for the link to close the ticket. -->
Closes FRC8592/2026-Rebuilt#

## What changed

<!-- A couple of sentences. What does the robot (or the build) do now that it didn't before? -->

## How I tested it

- [ ] `./gradlew build` — result:
      <!-- If it fails, say so and say whether the errors are only the known vendordep ones.
           See CONTRIBUTING.md; do not tick this box for a build you didn't run. -->
- [ ] Simulation — **if this changes a mechanism or an auto, how did you test it in the simulator?**
      <!-- What you ran, what you watched (which CustomLogs keys / AdvantageScope view), what you saw. -->
- [ ] Hardware — which robot, tethered or on the field?
      <!-- Leave unticked if it hasn't been on a robot yet. That's fine; just say so. -->

## Checklist

- [ ] Branch is `<INITIALS>/<issue#>-<slug>` and commits are `#<issue#> - <summary>`
- [ ] New numbers went into `Constants.java`, not inlined in the subsystem
- [ ] Logging goes through the subsystem's `LOG_PATH`
- [ ] Motor access goes through `TalonFXControl` / `SparkFlexControl`
- [ ] Nothing in `frc/robot/examples/` was wired into the robot
- [ ] Current and soft limits reviewed, if this moves a mechanism

## Notes for the reviewer

<!-- Anything you're unsure about, anything you want a second opinion on, anything left to do. -->
