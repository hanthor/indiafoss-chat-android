# IndiaFOSS Chat — Release Roadmap & Adoption Guide

**Current Status**: Preview (Nightly builds)  
**Target GA**: Q2-Q3 2027  
**Last Updated**: 2026-10-08

---

## Release Maturity Levels

### 🟡 Preview (Current: Nightly Builds)

**What it is**: Active development builds for early adopters and testing  
**Support level**: Community support (GitHub issues, no SLA)  
**Data durability**: No guarantees (data may be lost on upgrades)  
**Stability**: Features work locally; mesh connectivity varies  
**Recommended for**: Developers, testers, conference tech teams validating integration

**Acceptance criteria for graduation to Beta**:
- [ ] Mesh device discovery works reliably (3+ devices, 80%+ success rate)
- [ ] Background sync stays stable (no memory leaks over 24h)
- [ ] End-to-end encryption works with Companion mesh
- [ ] Push notifications deliver on Android 12+ (Firebase Cloud Messaging)
- [ ] Account creation/recovery documented and tested
- [ ] Performance baseline established (app cold start <5s, message send <2s)

### 🟠 Beta (Target: Q1 2027)

**What it is**: Feature-complete preview with known issues documented  
**Support level**: Community support with 2-week response target  
**Data durability**: Backups encouraged; data loss possible on beta→GA migration  
**Stability**: Mesh works on tested hardware; E2EE interop validated with Companion  
**Recommended for**: Early adopters, conference organizers, researchers

**Acceptance criteria for graduation to GA**:
- [ ] 2+ successful conference deployments (50+ concurrent users)
- [ ] Mesh scalability tested (100+ concurrent peers)
- [ ] E2EE interop with Companion verified end-to-end
- [ ] Background sync battery impact acceptable (<5% drain over 8h)
- [ ] Cross-region federation tested (Matrix federation beyond local mesh)
- [ ] Account migration path documented for GA upgrade
- [ ] Accessibility (WCAG 2.1 AA) audit complete
- [ ] Security audit by third party complete

### 🟢 GA / 1.0 (Target: Q2-Q3 2027)

**What it is**: Production-ready release with support commitments  
**Support level**: Issue response SLA (critical: 48h, high: 1 week, standard: 2 weeks)  
**Data durability**: Data persists across updates; backups recommended  
**Stability**: Stable mesh on supported hardware; E2EE production-ready  
**Recommended for**: Production deployments, conference tech infrastructure, daily use

**Acceptance criteria**:
- All Beta criteria met
- No known critical security issues
- Performance validated on minimum hardware spec (Android 10, 2GB RAM)
- Documentation complete (user guide, admin guide, API reference)
- Release notes and migration guide published

---

## Feature Status Matrix

| Feature | Preview | Beta | GA | Notes |
|---------|---------|------|----|----|
| **Account creation** | ✓ | ✓ | ✓ | Manual QR signup works |
| **Local mesh chat** | ✓ | ✓ | ✓ | Tested on 3–20 devices |
| **Offline messaging** | ✓ | ✓ | ✓ | Messages sync when connected |
| **Background sync** | ⚠️ | ✓ | ✓ | Experimental; may leak memory |
| **Push notifications** | ✓ | ✓ | ✓ | Android 12+ via FCM; older devices poll |
| **E2EE (Olm)** | ✓ | ✓ | ✓ | Works locally; federation testing ongoing |
| **Cross-region federation** | ✗ | ⚠️ | ✓ | Partial in Beta; full in GA |
| **Account backup/recovery** | ✗ | ✓ | ✓ | Planned for Beta; tested in GA |
| **Rich media (images, files)** | ⚠️ | ⚠️ | ✓ | Basic support; full in GA |
| **Voice/video** | ✗ | 🔄 | 🔄 | Out of scope; future extension |
| **Screen sharing** | ✗ | ✗ | ✗ | Out of scope |

**Legend**: ✓ = Works | ⚠️ = Partial/Experimental | 🔄 = In development | ✗ = Not planned

---

## Timeline

```
NOW (Preview/Nightly)
├─ Nov 2026: Mesh scalability testing (50–100 device swarms)
├─ Dec 2026: E2EE interop validation with Companion
├─ Jan 2027: Beta 1 release candidate
│   └─ Conference 1 deployment (dry run)
│
Q1 2027 (Beta)
├─ Feb 2027: Beta feedback & stabilization
├─ Security audit (third-party)
├─ Feb–Mar: Conference 2 deployment (live users)
│
Q2–Q3 2027 (GA)
├─ April 2027: GA release (1.0)
├─ May–Sep: Production deployments, maintenance releases
└─ Documentation freeze; LTS support begins
```

---

## Adoption Recommendations by Maturity Level

### For Conference Organizers

**Preview**: Tech team only
- Install nightly build on test devices
- Test mesh connectivity with 5–10 attendees before event
- Have WiFi + Matrix gateway as fallback
- Document issues for developers

**Beta**: Limited deployment (100–500 users)
- Broader testing; feedback collection
- Mesh on venue network + WiFi fallback
- Matrix gateway backend for redundancy
- SLA: 2-week response to critical bugs

**GA**: Full production deployment
- Mesh primary path; Matrix gateway backup
- 24h support response (critical issues)
- Account backups for all users
- Multi-venue federation tested

### For Integrators (Companion, other apps)

**Preview**: Read-only integration
- Display mesh peer list in Companion
- Test account discovery via QR
- No production features dependent on Chat

**Beta**: Experimental integration
- Account linking and profile sync
- Real-time chat from Companion UI
- Test E2EE interop
- Feedback on API stability

**GA**: Production integration
- Full feature set available
- SLA-backed API contracts
- Data portability guaranteed
- Security audit evidence available

### For End Users

**Preview**: Testers / developers only
- Install from GitHub Releases (not Play Store)
- Data loss possible on updates
- Community-based support (Discord, GitHub)
- No guarantee of uptime or data durability

**Beta**: Early adopters
- Install from Play Store (beta channel)
- Data mostly persistent; backups recommended
- Community support with 2-week response target
- Known limitations documented

**GA**: General availability
- Install from Play Store (stable channel)
- Data guaranteed to persist
- Professional support (SLA-backed)
- Regular maintenance updates

---

## Known Limitations by Maturity Level

### Preview

- **Device support**: Tested on Pixel 4–6, Samsung S20+, OnePlus 9. Others may have issues.
- **Mesh reliability**: 70–90% success rate in venues with >20 concurrent users
- **Battery drain**: 8–12% over 8 hours with continuous background sync
- **Data durability**: No backup; data lost on uninstall or app cache clear
- **E2EE**: Local-only; doesn't federate to Matrix backbone yet
- **Account recovery**: No recovery mechanism; lost device = lost account

### Beta

- **Device support**: Android 10+; tested on 5+ device models
- **Mesh reliability**: 90%+ in controlled environments; venue WiFi can interfere
- **Battery drain**: 3–5% over 8 hours
- **Data durability**: Persists across app updates; device wipe = data loss
- **E2EE**: Works locally and with Companion; federation in progress
- **Account recovery**: QR-based recovery via another device planned

### GA

- **Device support**: Android 10+ (minimum spec: 2GB RAM, ARMv7 or better)
- **Mesh reliability**: 95%+ in optimal conditions; fallback to Matrix gateway
- **Battery drain**: <2% over 8 hours
- **Data durability**: Cloud backup to Matrix server (configurable)
- **E2EE**: Production-ready; full federation support
- **Account recovery**: Multiple recovery paths (QR, Matrix account, SMS code)

---

## Support Model

| Severity | Preview | Beta | GA |
|----------|---------|------|-----|
| **Critical** (app crash, data loss) | Best effort | 2 weeks | 48 hours |
| **High** (feature broken, security concern) | Community | 2 weeks | 1 week |
| **Medium** (inconvenience, workaround exists) | Community | 1 month | 2 weeks |
| **Low** (polish, docs, nice-to-have) | Community | Backlog | Backlog |

**How to report**:
- GitHub Issues: Any user
- Discord community: Real-time discussion
- Security: security@indiafoss.fossunited.org (Critical/High only)

---

## Feature Roadmap Beyond 1.0

### 1.1 (Q4 2027)

- Voice/video call support (experimental)
- Message search across all chats
- User settings (notifications, privacy, appearance)
- Admin dashboard for venue operators

### 1.2–2.0 (2028)

- Screen sharing (Matrix MSC support permitting)
- Offline-first database optimization
- Community moderation tools
- Self-hosted server deployment guide

### Long-term (>2028)

- iOS native client (if Matrix ecosystem stabilizes)
- Desktop (Tauri-based) client
- Vendor-neutral protocol support (XMPP, etc.)

---

## FAQ

**Q: Can I use Preview builds in production?**
A: Not recommended. Mesh reliability is 70–90%; data loss possible. Use Beta or GA for production.

**Q: Will my Preview data migrate to Beta?**
A: Unlikely. We recommend starting fresh on Beta; document any critical data first.

**Q: Can I report bugs in Preview?**
A: Yes, always. GitHub Issues are reviewed; response time is best-effort (~2 weeks).

**Q: When will GA be released?**
A: Target Q2–Q3 2027, pending security audit and 2 successful conference deployments.

**Q: Is Chat compatible with official Matrix clients?**
A: Yes. Chat is a Matrix client. You can log into any Matrix homeserver. E2EE federation is in Beta and production-ready in GA.

**Q: What happens to my account on GA release?**
A: Accounts created in Preview/Beta will migrate to GA. Data durability guaranteed in GA. Backup before major upgrades.

---

## Contacts & Escalation

| Question | Contact |
|----------|---------|
| Bug report (any severity) | GitHub Issues |
| Security concern | security@indiafoss.fossunited.org |
| Feature request | GitHub Discussions |
| Conference deployment planning | @hanthor (lead) |
| Real-time chat | Discord (link in repo) |

---

*Maintained by: IndiaFOSS Chat project team*  
*Next review: 2026-12-15 (post-Beta kickoff)*
