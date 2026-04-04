# Contributing to LUMINA

*An open invitation from CHATRON 9.0 and Or4cl3 AI Solutions*

---

LUMINA exists for children who have nothing. Every contribution — whether a line of code, a translated story, a curriculum activity, or a review of our safeguarding logic — has the potential to reach a real child in a real crisis.

We welcome contributions from:

👩‍💻 **Developers** — Android, Kotlin, ML, on-device inference  
🌍 **Humanitarian workers** — NGO deployment expertise, field knowledge  
📚 **Educators** — Curriculum design, literacy, numeracy, trauma-informed pedagogy  
🗣️ **Translators** — Curriculum content in any of our priority languages  
🧠 **Child psychologists** — Review of HAVEN's trauma-informed interactions  
🔒 **Security researchers** — Audit of SENTINEL's safeguarding pipeline  

---

## Priority Contributions Right Now

### 1. Curriculum Content Packs
We urgently need content in:
- **Arabic** (`ar`) — largest refugee population need
- **Swahili** (`sw`) — East Africa / Great Lakes region
- **Hausa** (`ha`) — West Africa
- **Ukrainian** (`uk`) — displacement crisis
- **Pashto** (`ps`) — Afghanistan displacement
- **Burmese** (`my`) — Rohingya crisis

See `assets/curriculum/en_literacy_seedlings.json` for the content format.
Activities should be:
- Culturally grounded (local names, settings, references)
- Trauma-aware (no content that assumes a stable home)
- Age-appropriate (check the `ageGroup` field)

### 2. SENTINEL Pre-Screen Keywords
Sentinel's keyword lists need expansion for non-English languages.  
File: `src/agents/sentinel/SentinelAgent.kt`  
Please include a native speaker review for any non-English additions.

### 3. Safeguarding Review
If you have child protection or safeguarding expertise, please review:  
- `docs/DESIGN_PRINCIPLES.md`
- `src/agents/sentinel/SentinelAgent.kt`
- `src/agents/haven/HavenAgent.kt`

Open an issue with the label `safeguarding-review`.

### 4. NGO Sync Server
The NGO sync layer (`src/core/sync/NgoSyncManager.kt`) needs Phase 2 implementation:  
- mDNS service discovery (`_lumina._tcp.local`)
- REST sync protocol specification
- Bluetooth LE mesh for WiFi-less camp environments

---

## How to Contribute

1. Fork the repository
2. Create a branch: `git checkout -b feature/arabic-curriculum`
3. Make your changes
4. Write or update tests where applicable
5. Submit a Pull Request with a clear description

For curriculum contributions, no coding knowledge is needed — just edit the JSON files and submit a PR.

---

## Code Standards

- **Kotlin** — follow standard Android Kotlin style
- **No analytics SDKs** — LUMINA does not track children. Ever.
- **Privacy first** — any new data storage must be documented and justified
- **Trauma-informed** — any new child-facing content must follow `docs/DESIGN_PRINCIPLES.md`
- **Tests required** for any changes to ARIA, HAVEN, or SENTINEL agent logic

---

## Safeguarding Commitment

All contributors must understand and agree to LUMINA's safeguarding principles:

- Children's data is never transmitted without explicit consent
- SENTINEL's safety logic is never weakened or bypassed
- Any content targeting children must be reviewed before merge
- Suspected safeguarding issues in the codebase should be reported privately to: **security@or4cl3.ai**

---

## Questions?

Open an issue or reach out: **lumina@or4cl3.ai**

Thank you for helping us put a light in the dark. 🌟

*— CHATRON 9.0 | Or4cl3 AI Solutions*
