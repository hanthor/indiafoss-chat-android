# IndiaFOSS Chat roadmap: P2P mesh messaging for conferences

Updated 2026-10-04. This is the public adoption and release roadmap for IndiaFOSS Chat — a P2P-enabled Element X Android fork providing mesh messaging for the IndiaFOSS Companion ecosystem and beyond.

## Current status

**Phase: Preview and acceptance testing**

The app has:
- ✓ Offline-first conference integration (Companion PWA hosted in native WebView)
- ✓ E2EE messaging on public homeservers (Element X Matrix encryption)
- ✓ P2P mesh messaging via pinned Neutrino bindings (`0.8.2-e2ee.2d85348-ble.15117e9`)
- ✓ BLE mesh transport and nearby-discovery toggle
- ✓ Contact card handoff from the Companion (friend card → matrix.to → native profile screen)
- ✓ Deep link integration (`indiafoss://chat?dm=@user:server`, `indiafoss://friend?v=1…`)
- ✓ Durable outbox with queued/accepted/read/uncertain message states
- ✓ Arm64-v8a preview APK (nightly release with checksums and release manifest)
- ✓ Developer onboarding, design docs, and build instructions

**Acceptance status**:
- Preview: arm64-v8a APK available for testing; universal APK also published (larger)
- Mesh E2EE: Bindings implemented; device interoperability, media recovery, and background operation acceptance pending (#45, #49)
- Background and recovery: Tracked in #45 (mesh messaging persistence) and #49 (device recovery scenarios)
- Physical device testing: In progress; reports welcomed

**Release posture**: Not production-ready. Preview app for conference attendees and mesh developers; data loss or service interruption possible.

## Near-term: October 2026–January 2027

### 1. Complete mesh interoperability acceptance gates

**Goal**: Validate that mesh messaging works reliably across real devices in conference scenarios.

**Scope**:
- Device-to-device messaging without a server (#45): test BLE peer discovery, message routing, and offline queuing
- Media over mesh (#45): voice notes and media attachments on P2P connections
- Device recovery (#49): re-join mesh after app restart, network switch, or device unlock
- Background operation: verify that notifications and message sync work with app backgrounded or killed
- Battery and CPU impact: measure resource usage on real devices over 4-hour conference day

**Success criteria**:
- All four scenarios pass on at least one device class (e.g., Pixel 7 Pro, Samsung Galaxy S24)
- Mesh messaging works for ≥90% of send attempts (accounting for BLE range, interference, device sleep)
- No crashes or data loss in the outbox over 24-hour continuous operation
- Battery impact <5% increase vs. non-mesh messaging

### 2. Expand architecture support

**Goal**: Enable arm64-v8a, x86_64, and armeabi-v7a users.

**Scope**:
- Test and verify Neutrino bindings on x86 (emulator) and 32-bit ARM devices
- Document which device classes are supported and which have known gaps (GPU driver issues, BLE support)
- Publish architecture-specific APKs alongside the universal APK
- Update the getting-started guide with device recommendations

**Success criteria**:
- x86_64 and armeabi-v7a preview APKs published with architecture-specific release notes
- At least one real 32-bit device tested and documented

### 3. Establish measurable adoption signals

**Goal**: Understand how users discover and use IndiaFOSS Chat; identify barriers.

**Scope**:
- Publish a getting-started guide for:
  - Downloading the preview APK (QR code, direct link, GitHub releases)
  - Configuring Neutrino server settings (P2P mesh server vs. public homeserver)
  - Scanning friend cards from the Companion
  - Joining conference chat rooms
- Collect adoption metrics:
  - Preview APK downloads (per week)
  - GitHub releases page views
  - User feedback from issue reports (bug reports, feature requests, usability friction)
  - Mesh adoption rate (% of active users with BLE discovery enabled)
- Target: ≥5 external users running the preview; ≥2 reproducible external feedback items

**Success criteria**:
- Getting-started guide is published and tested by at least 2 external users
- Monthly download metrics are tracked and reported
- At least 3 external user feedback issues are filed and triaged

### 4. Coordinate release manifest and Companion integration

**Goal**: Align Chat, Companion, and Neutrino release timelines for conferences.

**Scope**:
- Maintain the release manifest tying Neutrino, neutrino-iroh, Chat bindings `.aar`, Chat commit, and signed APK
- Coordinate with indiafoss-companion on release tags and Neutrino server deployment
- Document the dependency chain and update process for maintainers
- Test Companion → Chat deep links and contact card handoff in end-to-end scenarios

**Success criteria**:
- Release manifest is kept current with each Neutrino and Chat release
- Companion → Chat integration is tested before each conference event
- Maintainers can reproduce the full release chain (Neutrino → bindings → Chat APK) in <30 minutes

## Mid-term: January–April 2027

### 1. Daily-driver candidate release

**Goal**: Move from preview to a stable release candidate suitable for conference use and broader testing.

**Scope**:
- Complete all acceptance gates from near-term phase (#45, #49)
- Publish a signed, reproducible release build (not nightly)
- Establish a security and maintenance SLA (e.g., security patches within 48 hours)
- Document known limitations and unsupported features
- Transition from nightly to scheduled releases (e.g., monthly or per-conference)

**Success criteria**:
- All acceptance gates (#45, #49) are closed with evidence
- A stable release APK is published with semantic versioning (e.g., v0.1.0)
- Security.md is published with responsible disclosure process
- Release notes document known issues and supported device configurations

### 2. Upstream contribution and interoperability

**Goal**: Coordinate with Element and Neutrino upstream; establish interop with standard Matrix clients.

**Scope**:
- Test message compatibility: messages sent from IndiaFOSS Chat to Element X, Riot, and standard Matrix clients
- Identity and display name handling: ensure @localpart:server parsing is consistent
- E2EE device trust: test identity verification and cross-signing flows
- Contribute mesh-specific fixes back to Neutrino if they are of general interest

**Success criteria**:
- Messages sent from IndiaFOSS Chat are readable in Element X, Element Web, and Riot Android
- Device identity is preserved across client switches (no re-verification loops)
- At least one upstream contribution (patch, issue report, or test case) is merged into Neutrino

### 3. Multi-conference support and event targeting

**Goal**: Support multiple conferences (not just IndiaFOSS); coordinate with event organizers.

**Scope**:
- Generalize the Companion integration (currently hardcoded for IndiaFOSS 2026)
- Document how event organizers can:
  - Deploy a custom Neutrino instance for their conference
  - Configure Chat for their event (branding, server URL, conference data source)
  - Distribute APKs and getting-started guides to attendees
- Test Chat + Companion on a second conference or event

**Success criteria**:
- Chat can be configured for a non-IndiaFOSS event without code changes
- At least one additional event (e.g., FOSDEM 2027) is documented as supported
- Event-specific configuration guide is published

## Six to twelve months: April–October 2027

### Readiness gates for 1.0 release

A 1.0 release of IndiaFOSS Chat requires:

1. **Mesh interoperability**: All acceptance gates (#45, #49) closed; device-to-device messaging works reliably
2. **Security and maintenance**: Security.md published; vulnerability response SLA established; signed releases with reproducible builds
3. **Adoption evidence**: ≥100 active preview users; ≥10 external contributors or reporters with documented use cases
4. **Integration validation**: End-to-end testing with Companion, Neutrino, and at least one downstream event (FOSDEM, FOSSConf, etc.)
5. **Documentation**: All technical and user-facing guides complete and verified by external users
6. **Upstream coordination**: No blocker issues with Element or Neutrino; interop with standard Matrix clients confirmed

### Optional future work

- **Public extension API**: Allow third-party apps to integrate Chat messaging (consider post-1.0)
- **Audio/video calling**: SFU-based or P2P calling over mesh (larger feature; post-1.0)
- **Voice notes over mesh**: Full media support for P2P connections (in progress, part of #45)
- **Offline drafts and sync**: Persist drafts and sync across devices (future optimization)
- **Custom server UI**: Allow users to visually configure their Neutrino server (post-1.0)

## Known limitations

- **P2P scope**: Currently scoped for single-conference attendee networks; internet-scale mesh is not a goal
- **Device support**: Android 7.0+ only; iOS not planned
- **Architecture coverage**: x86_64 and armeabi-v7a untested; arm64-v8a is primary
- **Media over mesh**: Voice notes work; photos/videos over P2P still in acceptance (#45)
- **Background operation**: BLE and notifications need hardening for suspended devices
- **Server deployment**: Neutrino instance deployment is manual; no automated deployment guide yet

## How contributors can help

- **Mesh interoperability testing** (#45, #49): Test device-to-device messaging, media, and recovery on your devices; document exact device class, OS version, and BLE chipset
- **Architecture testing**: Try the preview APK on x86 emulator or 32-bit devices; report crashes or usability issues
- **Integration feedback**: Use Chat + Companion together; report friction points and compatibility issues
- **Getting-started guide review**: Test the onboarding docs; identify unclear steps or missing information
- **Upstream coordination**: Report issues found with Element X, Matrix SDK, or Neutrino; contribute fixes if applicable
- **Event coordination**: Help deploy Chat for a second conference; document the process and identify pain points

See [developer onboarding](docs/_developer_onboarding.md), [design](docs/design.md), and [building](README.md#building) for technical details. Report mesh and acceptance issues in #45 and #49; file general issues for bugs, documentation, or features.

## Release history

| Version | Date | Status | Notes |
|---------|------|--------|-------|
| Preview (nightly) | 2026-10-04 | Active | arm64-v8a APK; E2EE on public servers; mesh bindings included; acceptance testing in progress |
| 0.1.0 (candidate) | Planned Q1 2027 | Pending gates | All acceptance gates complete; daily-driver ready; stable release |
| 1.0 (production) | Planned Q4 2027 | Pending gates | Upstream coordination, adoption evidence, multi-conference support |

---

*Last updated 2026-10-04 by strategist agent (ACMM L5 — hold-gated mode)*
