# Release Readiness and Adoption Roadmap

This document outlines the maturity levels for IndiaFOSS Chat, acceptance criteria for each level, and the roadmap from preview to general availability.

## Release Maturity Levels

### Level 1: Preview (Current)

**Status:** Nightly APK builds available at https://github.com/hanthor/indiafoss-chat-android/releases/tag/nightly

**Characteristics:**
- Frequent builds (daily or multiple times per day)
- Features may be incomplete or experimental
- Known bugs and missing features documented in issues
- No upgrade guarantee (APK from one day to next may break app state)
- Not recommended for production use

**Acceptance Criteria Met:**
- ✅ Builds on Android 7.0+
- ✅ App launches and connects to conference Spindle
- ✅ Public homeserver messaging works (non-encrypted)
- ✅ Neutrino mesh E2EE implemented (desktop tested)
- ✅ Integration with Companion PWA (deeplinks, conference handoff)

**Known Gaps (Tracked in #45, #49):**
- ❌ Physical device mesh (currently desktop/emulator only)
- ❌ Background connectivity (app must stay in foreground)
- ❌ Device interoperability (cross-device key sharing)
- ❌ Recovery after app restart
- ❌ Notifications (background message delivery)

**User SLA:** None. Preview is experimental; no support guarantee.

**Lifespan:** Indefinite; nightly builds continue until Level 2 (Beta) is released.

### Level 2: Beta (Planned)

**Target Date:** Q1 2027 (provisional; gates must pass first)

**Characteristics:**
- Stable APK releases (weekly or monthly, not daily)
- Feature-complete for conference use case
- App state persists across restarts
- Upgrade path from previous beta version
- Recommended for testing but not production

**Acceptance Criteria Required:**
- ✅ Physical device mesh (two real phones, E2EE messages delivered)
- ✅ Background connectivity (app doesn't need to stay in foreground)
- ✅ Device recovery (app state preserved after force-stop and restart)
- ✅ Notifications (background message delivery with sound/vibration)
- ✅ Cross-device interoperability (keys shared between native and PWA)
- ✅ User guide and troubleshooting (docs/USER_GUIDE.md)
- ✅ Automated test coverage (integration tests for critical paths)

**User SLA:** Best-effort bug fixes within 2 weeks; security fixes within 24 hours.

**Upgrade Path:** Beta users can upgrade within the beta channel; breaking changes announced 1 week prior.

**Lifespan:** ~6 months or until Level 3 (GA) is released.

### Level 3: General Availability (GA / 1.0)

**Target Date:** Q2-Q3 2027 (provisional; depends on beta feedback)

**Characteristics:**
- Stable, production-ready releases (monthly or quarterly)
- All major features complete and tested
- App state fully durable (survives crashes, restarts, network loss)
- Guaranteed upgrade path (forward-compatible for 2 major versions)
- Supported on all Android versions 7.0+

**Acceptance Criteria Required:**
- ✅ All Beta criteria met
- ✅ Performance benchmarks pass (p95 message latency < 2 sec)
- ✅ Security audit completed (E2EE, device verification, Neutrino mesh)
- ✅ Accessibility audit (AT-SPI compliance for screen readers)
- ✅ Translation infrastructure in place (i18n setup, community translations)
- ✅ User documentation complete (in-app help, FAQ, troubleshooting)
- ✅ 99% uptime SLA for public servers (Spindle + conference rooms)

**User SLA:**
- Bug fixes: 2-week SLA
- Security fixes: 24-hour SLA
- Critical data loss: incident response within 4 hours
- Maintenance window: Announced 2 weeks prior

**Support Channels:**
- GitHub issues (primary)
- Email: support@indiafoss.org (when available)

**Upgrade Path:** Forward-compatible for 2 major versions; deprecation notice 3 months before EOL.

**Lifespan:** Indefinite; LTS (long-term support) until successor replaces it.

## Feature Status Matrix

| Feature | Preview | Beta | GA | Notes |
|---------|---------|------|-----|-------|
| **Core Messaging** | | | | |
| Text messages | ✅ | ✅ | ✅ | Fully implemented |
| Media sharing | ⚠️ | ✅ | ✅ | Preview: limited to images |
| Message reactions | ❌ | ⚠️ | ✅ | Beta: planned for Q1 2027 |
| Message search | ❌ | ❌ | ✅ | Out of scope for Beta |
| **Encryption** | | | | |
| E2EE (Megolm) | ✅ | ✅ | ✅ | Public homeservers only |
| Device verification | ⚠️ | ✅ | ✅ | Preview: manual only |
| Key backup/recovery | ❌ | ⚠️ | ✅ | Beta: Keychain support |
| Cross-device key sharing | ❌ | ✅ | ✅ | Beta: via verified devices |
| **Mesh & P2P** | | | | |
| Mesh discovery (mDNS) | ✅ | ✅ | ✅ | Local network only |
| Mesh E2EE | ✅ | ✅ | ✅ | Desktop/emulator tested |
| BLE mesh transport | ⚠️ | ✅ | ✅ | Preview: emulator only |
| Device interop | ❌ | ✅ | ✅ | Beta: native ↔ PWA |
| **Reliability** | | | | |
| Offline mode | ✅ | ✅ | ✅ | Read-only in preview |
| Message queue/retry | ⚠️ | ✅ | ✅ | Preview: limited |
| Background sync | ❌ | ✅ | ✅ | Beta: systemd-user service |
| App crash recovery | ⚠️ | ✅ | ✅ | Preview: data loss possible |
| **User Experience** | | | | |
| Notifications | ❌ | ✅ | ✅ | Beta: FCM + local |
| Deep linking | ✅ | ✅ | ✅ | Conference handoff |
| Settings/preferences | ✅ | ✅ | ✅ | Basic; expanding in GA |
| Accessibility (a11y) | ⚠️ | ⚠️ | ✅ | Preview/Beta: partial |
| Multi-language | ❌ | ⚠️ | ✅ | Beta: English + Hindi |

**Legend:**
- ✅ = Fully implemented and tested
- ⚠️ = Partially implemented or limited functionality
- ❌ = Not implemented

## Roadmap Timeline

```
NOW                                    Q1 2027              Q2-Q3 2027          Beyond
|                                      |                    |                    |
Preview (nightly)                      Beta (monthly)       GA / 1.0 (quarterly) Maintenance
├─ Mesh E2EE (desktop)                 ├─ Physical mesh ✓   ├─ Security audit    ├─ Security updates
├─ Neutrino bindings ✓                 ├─ Background ✓      ├─ Perf benchmarks   ├─ i18n expansion
├─ Companion handoff ✓                 ├─ Device recovery   ├─ Accessibility     ├─ New features
├─ Public server chat ✓                ├─ Notifications     ├─ Translation       └─ LTS support
└─ Conference integration               ├─ Cross-device keys ├─ User docs
                                        ├─ User guide        └─ Public release
                                        └─ Integration tests
```

## Adoption Recommendations

### For Conference Organizers (IndiaFOSS)

**Current (Preview):**
- ✅ Use for testing venue mesh and E2EE
- ✅ Test with Companion app on desktops/laptops
- ❌ Don't rely on for production messaging yet
- ⚠️ Expect data loss and frequent restarts

**Beta (Q1 2027):**
- ✅ Deploy for attendee messaging at venue
- ✅ Use with real devices (phones)
- ⚠️ Have fallback plan (web chat) if issues arise
- ✅ Collect feedback for GA improvements

**GA (Q2-Q3 2027):**
- ✅ Recommended for production deployment
- ✅ SLA-backed support
- ✅ Stable upgrade path

### For App Integrators (Companion, etc.)

**Current (Preview):**
- ✅ Deep linking works (handoff to chat)
- ✅ Mesh identity federation works
- ❌ Don't assume reliability for critical flows
- ⚠️ Plan for fallback (web chat)

**Beta (Q1 2027):**
- ✅ Can integrate as primary chat experience
- ✅ Notifications and background sync work
- ⚠️ Handle edge cases (device loss, key recovery)
- ✅ Test cross-device flows

**GA (Q2-Q3 2027):**
- ✅ Tight integration recommended
- ✅ All edge cases handled
- ✅ Documented API stability

### For Users

**Current (Preview):**
- ✅ Download and test for conference use
- ❌ Don't store critical data (may be lost)
- ⚠️ Expect crashes and bugs
- ✅ Report issues on GitHub

**Beta (Q1 2027):**
- ✅ Daily driver for conference attendees
- ✅ Messages persist across restarts
- ⚠️ Backup important conversations (export)
- ✅ Feedback shape final feature set

**GA (Q2-Q3 2027):**
- ✅ Recommended for all users
- ✅ Data durability guaranteed
- ✅ Professional support channel

## Known Limitations (Preview)

### Hardware

- **Mesh on real devices:** BLE mesh transport not tested on physical phones; emulator testing only
- **Background behavior:** App must stay in foreground; no systemd-user service yet
- **Battery:** Mesh discovery (mDNS/BLE broadcast) may drain battery; optimization needed

### Software

- **App crashes:** Force-stop may cause data loss (not yet persisted to disk)
- **Device interop:** Keys not shared between native app and PWA; each has separate identity
- **Notifications:** No background message delivery; only works when app is open
- **Media:** Image sharing works; audio/video not yet supported

### Connectivity

- **Offline mode:** App can display cached messages but cannot send until connected
- **Network switch:** Switching from WiFi to cellular may drop connections (retry needed)
- **Gateway failover:** If venue gateway is down, messages queued but not delivered until it returns

## Reporting Issues

### Preview Issues

File on GitHub: https://github.com/hanthor/indiafoss-chat-android/issues

Include:
- Android version and device model
- App version (from About screen)
- Steps to reproduce
- Screenshot or error log
- Whether this is a blocker for conference use

### Beta Issues

Same as preview; add priority (P0: blocker, P1: high, P2: medium, P3: low)

### GA Critical Issues

Same as beta; additionally:
- Contact support@indiafoss.org for immediate response
- Incidents (data loss, security) get 4-hour response SLA

## Questions?

- Architecture questions: See `docs/_developer_onboarding.md` and `docs/design.md`
- Neutrino questions: See hanthor/neutrino repository
- Release questions: Open an issue and label it `roadmap`
- User support: GitHub issues (community support) or support@indiafoss.org (when available)
