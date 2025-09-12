# SteamaTip AI - Scoring System Flowchart

## Visual Flow Diagram

```
                    ┌─────────────────────────────────────┐
                    │         HORSE ANALYSIS START        │
                    └─────────────────┬───────────────────┘
                                      │
                                      ▼
                    ┌─────────────────────────────────────┐
                    │        Has Real Form Data?          │
                    └─────────────────┬───────────────────┘
                                      │
                        ┌─────────────┴─────────────┐
                        │                           │
                        ▼                           ▼
            ┌─────────────────────┐    ┌─────────────────────┐
            │        NO           │    │        YES          │
            │                     │    │                     │
            │   EXCLUDE HORSE     │    │  Check Horse Type   │
            │   (No Score)        │    │                     │
            └─────────────────────┘    └─────────┬───────────┘
                                                 │
                        ┌────────────────────────┼────────────────────────┐
                        │                        │                        │
                        ▼                        ▼                        ▼
        ┌─────────────────────────┐  ┌─────────────────────────┐  ┌─────────────────────────┐
        │     FIRST STARTER       │  │      SPELL HORSE        │  │     NORMAL HORSE        │
        │                         │  │                         │  │                         │
        │  (No race history)      │  │  (12+ weeks break)      │  │  (Regular form)         │
        └─────────┬───────────────┘  └─────────┬───────────────┘  └─────────┬───────────────┘
                  │                            │                            │
                  ▼                            ▼                            ▼
        ┌─────────────────────────┐  ┌─────────────────────────┐  ┌─────────────────────────┐
        │   5 LAWS (50 pts max)   │  │  10 LAWS (108 pts max)  │  │   9 LAWS (116 pts max)  │
        │                         │  │                         │  │                         │
        │ • Jockey (8 pts)        │  │ • 1st Up OR 2nd Up (8)  │  │ • Recent Form (25 pts)  │
        │ • Trainer (8 pts)       │  │ • Class Suitability (25) │  │ • Class Suitability (25) │
        │ • Barrier (6 pts)       │  │ • Track/Distance (20)   │  │ • Track/Distance (20)   │
        │ • Trial Sectionals (10) │  │ • Sectional Time (8)    │  │ • Sectional Time (8)    │
        │ • Jockey-Horse (8 pts)  │  │ • Barrier (6 pts)       │  │ • Barrier (6 pts)       │
        │                         │  │ • Jockey (8 pts)        │  │ • Jockey (8 pts)        │
        │                         │  │ • Trainer (8 pts)       │  │ • Trainer (8 pts)       │
        │                         │  │ • Jockey-Horse (8 pts)  │  │ • Jockey-Horse (8 pts)  │
        │                         │  │ • Track Condition (8)   │  │ • Track Condition (8)   │
        └─────────┬───────────────┘  └─────────┬───────────────┘  └─────────┬───────────────┘
                  │                            │                            │
                  ▼                            ▼                            ▼
        ┌─────────────────────────┐  ┌─────────────────────────┐  ┌─────────────────────────┐
        │   Calculate Total       │  │   Calculate Total       │  │   Calculate Total       │
        │   Score (Max 50)        │  │   Score (Max 108)       │  │   Score (Max 116)       │
        └─────────┬───────────────┘  └─────────┬───────────────┘  └─────────┬───────────────┘
                  │                            │                            │
                  └────────────────────────────┼────────────────────────────┘
                                               │
                                               ▼
                                    ┌─────────────────────────┐
                                    │   Apply Tie Resolution  │
                                    │  (Win/Place Ratio)      │
                                    └─────────┬───────────────┘
                                              │
                                              ▼
                                    ┌─────────────────────────┐
                                    │ Generate Betting        │
                                    │ Recommendation          │
                                    │                         │
                                    │ • Super Bet (8+ gap)    │
                                    │ • Best Bet (5-7.9 gap)  │
                                    │ • Good Bet (3-4.9 gap)  │
                                    │ • Consider (<3 gap)     │
                                    └─────────┬───────────────┘
                                              │
                                              ▼
                                    ┌─────────────────────────┐
                                    │    Display Results      │
                                    │                         │
                                    │ • Ranked by Score       │
                                    │ • Betting Indicators    │
                                    │ • Detailed Breakdown    │
                                    └─────────────────────────┘
```

## Spell Horse Sub-Flowchart

```
                    ┌─────────────────────────────────────┐
                    │         SPELL HORSE DETECTED        │
                    └─────────────────┬───────────────────┘
                                      │
                                      ▼
                    ┌─────────────────────────────────────┐
                    │      Analyze Form String            │
                    │      (Read Right-to-Left)           │
                    │                                     │
                    │  Example: "32x1x2212x"              │
                    │  Reading: x, 2, 1, 2, x, 1, x, 2, 3 │
                    └─────────────────┬───────────────────┘
                                      │
                        ┌─────────────┴─────────────┐
                        │                           │
                        ▼                           ▼
            ┌─────────────────────┐    ┌─────────────────────┐
            │     1ST UP          │    │     2ND UP          │
            │                     │    │                     │
            │ Most recent = 'x'   │    │ Pattern = "num + x" │
            │ (Spell detected)    │    │ (e.g., "2x")        │
            └─────────┬───────────┘    └─────────┬───────────┘
                      │                           │
                      ▼                           ▼
            ┌─────────────────────┐    ┌─────────────────────┐
            │   1ST UP LAWS       │    │   2ND UP LAWS       │
            │                     │    │                     │
            │ • 1st Up (ACTIVE)   │    │ • 1st Up (inactive) │
            │ • 2nd Up (inactive) │    │ • 2nd Up (ACTIVE)   │
            │ • Sectional (inact) │    │ • Sectional (ACTIVE)│
            │ • All other laws    │    │ • All other laws    │
            │   same as normal    │    │   same as normal    │
            └─────────────────────┘    └─────────────────────┘
```

## Key Decision Points

### 1. Form Data Validation
- **Real Data Required**: Only horses with complete, real form data are scored
- **No Mock Data**: System fails gracefully rather than using fake data
- **Data Integrity**: All form strings, race results, and statistics must be genuine

### 2. Horse Categorization
- **First Starter**: No race history + has trial data
- **Spell Horse**: 12+ weeks since last race OR form string shows spell pattern
- **Normal Horse**: Regular racing with recent form

### 3. Scoring Adaptation
- **Spell Horses**: Exclude Recent Form law, add 1st Up/2nd Up laws
- **First Starters**: Limited to 5 laws due to no race history
- **Normal Horses**: Full 9-law scoring system

### 4. Betting Recommendations
- **Point Gap Analysis**: Based on difference between 1st and 2nd place
- **Confidence Levels**: Super Bet (8+), Best Bet (5-7.9), Good Bet (3-4.9)
- **Visual Indicators**: Color coding and highlighting for easy identification

---

*This flowchart provides a comprehensive visual guide to the SteamaTip AI scoring system, showing how horses are categorized and scored based on their individual circumstances.*
