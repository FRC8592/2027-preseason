# Contributing

How software work gets done on FRC Team 8592. This file is about **process** — tickets, branches,
pull requests, and what a reviewer looks for. For setup, Gradle commands, architecture and code
conventions, read [`README.md`](README.md) first.

If something here disagrees with what someone told you verbally, this file wins. If this file is
wrong, open a ticket and fix it.

## Before you start

Work starts from a ticket.

1. **Tickets live in [FRC8592/2026-Rebuilt](https://github.com/FRC8592/2026-Rebuilt/issues)** — that
   is the team's tracker, including for work in this repo. (Branch names and commit subjects here
   already reference its numbers, like `#177` and `#215`.)
2. **Claim the ticket** before you write code: assign yourself and leave a comment saying you are
   picking it up. That is how we avoid two people building the same thing.
3. If no ticket covers what you want to do, **open one** (see [New issues](#new-issues)) or ask in the
   software channel. A ticket is cheap; a surprise 400-line PR is not.

## Branches

```
<INITIALS>/<issue#>-<slug>
```

Your initials, a slash, the ticket number, then a short dash-separated description.

```
BS/215-add-contributing-and-templates
AC/220-fix-the-bugs-in-the-2026-motor-helpers
```

- Cut the branch from an up-to-date `main` (`git switch main && git pull && git switch -c ...`).
- **Never commit directly to `main`.** Everything goes through a pull request.
- One ticket per branch. If you find a second problem while you are in there, open a ticket for it.

## Commits

Subject line:

```
#<issue#> - <summary>
```

For example `#215 - Add CONTRIBUTING.md and PR/issue templates`.

- Write the summary in the imperative ("Add the sim question", not "Added" or "Adding").
- Several small commits are better than one giant one — it makes review and `git bisect` possible.
- **Do not force-push a branch that someone is reviewing.** Push new commits on top instead; the
  reviewer loses their place otherwise. Force-push is fine before you ask for review.

## Building and checking your work

Run Gradle **from the repository root**:

```bash
./gradlew build          # compile + test, the same thing CI runs
./gradlew simulateJava   # simulation with GUI + driver station
./gradlew deploy         # SystemCore, team 8592
```

> [!IMPORTANT]
> In this repo the robot project **is** the repo root. The 2026 repo
> ([FRC8592/2026-Rebuilt](https://github.com/FRC8592/2026-Rebuilt)) keeps its code in a
> `robot-production/` subdirectory, so commands and CI config copied from there will not work here.

**Test in simulation before you test on the robot.** If your change touches a mechanism or an auto,
drive it in `simulateJava` and say in the PR what you saw. Open the resulting `.wpilog` in
[AdvantageScope](https://docs.advantagescope.org) and check the log keys for the subsystem you
touched (`CustomLogs/<SUBSYSTEM>/...`) rather than trusting that it "looked fine".

### The build is red right now — that is expected

`./gradlew build` currently fails with roughly 2800 compile errors, and CI fails too. **This is not
your fault and not a regression.** The 2027-alpha vendordeps for Phoenix 6, REVLib, PathPlannerLib,
AdvantageKit and PhotonLib do not exist yet; see the migration notes in [`README.md`](README.md) for
the full story, and note that the CI workflow additionally still points at a `robot-production/`
directory that this repo does not have.

While that is true:

- Say what actually happened in the PR. **Do not tick a box claiming a green build you could not
  have gotten.** "Build fails with the known vendordep errors, no new ones" is a useful, honest
  answer — and worth checking: compare the error count before and after your change.
- Keep changes small and readable, because review is the only real gate until the build is fixed.

## Pull requests

- PRs target **`main`**.
- Fill in the template. Empty PR bodies are the reason this file exists.
- **Link the ticket.** Because the tracker is a different repo, a bare `#215` does not close
  anything — write the full form:

  ```
  Closes FRC8592/2026-Rebuilt#215
  ```

- **One approval from a software lead** is required to merge, plus a green CI build once CI itself is
  working again. [`.github/CODEOWNERS`](.github/CODEOWNERS) requests those reviewers automatically;
  note that it only *requests* review — until branch protection is turned on for `main`, the
  approval and build rules are team policy, not something GitHub enforces. Follow them anyway.
- Open it as a **draft** if it is not ready. A draft PR with a question in it is a perfectly good way
  to ask for help.

## Reviewing a pull request

Reviewing is not rubber-stamping, and anyone on the software team can review. Things worth checking,
roughly in order of how much damage they do:

- **Anything that moves:** are current limits and soft limits set, and do the limits in
  `Constants.java` still make sense for the new motion? A missing limit breaks hardware.
- **No blocking code in the robot loop:** no `sleep`, no `while` loop waiting on a sensor, no
  unbounded wait inside `periodic()` or a command. The scheduler runs every 20 ms and will not wait
  for you.
- **Units and frames:** rotations vs radians vs degrees, meters vs inches, field- vs robot-relative,
  and which alliance the pose assumes.
- **Numbers in the right place:** new constants belong in `Constants.java`, not inlined at the call
  site. Remember that some values are live-tuned from SmartDashboard and read back every loop, so
  changing a default is not always enough to change behavior.
- **Was it driven in sim?** If the PR says no and it touches a mechanism or an auto, ask.
- **Logging:** new state worth seeing later should be logged through the subsystem's `LOG_PATH`.

Leave the actual approval to a lead, but comments from everyone are welcome — reading other people's
code is the fastest way to learn this codebase.

## On the robot

- Simulation first, hardware second.
- Before anyone enables: tether or connect, say out loud that you are enabling, and make sure the
  area around the robot is clear.
- During build season, **deploy only from a reviewed branch** (or `main`) once the robot is being
  used for practice. Deploying your work-in-progress onto a robot a driver is about to drive is how
  practice time disappears.
- `BuildConstants` records the branch and commit that got deployed — check it when the robot is not
  behaving like the code you think is on it.

## Code conventions

The full list is in [`README.md`](README.md); the short version:

- `Constants.java` is the single source of CAN IDs, limits, gains, field geometry and LED colors.
- Log through the subsystem's `LOG_PATH` constant, not ad-hoc strings.
- Motors go through `helpers/TalonFXControl` and `helpers/SparkFlexControl` (`Turret` is the
  documented exception).
- New controls are named `Trigger` fields in `RobotContainer`, not inline `controller.x()` calls.
- Prefer the wrappers in `commands/proxies/` over re-deriving their scheduler behavior.
- `frc/robot/examples/` is teaching reference code. Do not wire it into the robot, and do not "fix"
  it to match the production classes.
- **Assume any FRC example written for 2026 or earlier is wrong about package names** — 2027 renamed
  `edu.wpi.first.*` to `org.wpilib.*`.

## New issues

Use a template at
[new issue](https://github.com/FRC8592/2027-preseason/issues/new/choose) — **Task** for work to do,
**Robot bug** for something the robot did wrong. Fill in every heading; "Definition of done" and
"How we'll test it" are the ones that make a ticket actionable by somebody other than you.

## Getting help

Ask in the software channel, ask a lead, or open a draft PR with your question in the description.
Asking early is cheaper than a rewrite.
