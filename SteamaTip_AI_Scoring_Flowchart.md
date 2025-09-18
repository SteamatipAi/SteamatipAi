# SteamaTip AI - Complete System Flowchart

## User Flow Diagram

```
                    ┌─────────────────────────────────────┐
                    │         SELECT DATE                 │
                    └─────────────────┬───────────────────┘
                                      │
                                      ▼
                    ┌─────────────────────────────────────┐
                    │       SELECT TRACKS                 │
                    │    (Multi-selection with checkboxes)│
                    └─────────────────┬───────────────────┘
                                      │
                        ┌─────────────┴─────────────┐
                        │                           │
                        ▼                           ▼
            ┌─────────────────────┐    ┌─────────────────────┐
            │    ANALYSE BUTTON   │    │   BEST BETS BUTTON  │
            │                     │    │                     │
            │ • Complete Analysis │    │ • Filtered Analysis │
            │ • All Horses        │    │ • Only Green/Blue/  │
            │ • Race-by-Race      │    │   Purple Horses     │
            └─────────┬───────────┘    └─────────┬───────────┘
                      │                           │
                      ▼                           ▼
            ┌─────────────────────┐    ┌─────────────────────┐
            │   RACE SELECTION    │    │   BEST BETS RESULTS │
            │                     │    │                     │
            │ • Choose Race       │    │ • Grouped by Track  │
            │ • See All Races     │    │ • Clickable Horses  │
            └─────────┬───────────┘    │ • Excel Export      │
                      │                │ • Text Sharing      │
                      ▼                └─────────────────────┘
            ┌─────────────────────┐              
            │  COMPLETE RACE FIELD│              
            │                     │              
            │ • All Horses Listed │              
            │ • Clickable Details │              
            │ • Share All Results │              
            └─────────┬───────────┘              
                      │                          
                      ▼                          
            ┌─────────────────────┐              
            │  HORSE DETAIL VIEW  │              
            │                     │              
            │ • Complete Scoring  │              
            │ • Law Breakdown     │              
            │ • Form Analysis     │              
            └─────────────────────┘              
```

## Horse Analysis Flow Diagram

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

## Best Bets Feature Flow

```
                    ┌─────────────────────────────────────┐
                    │       BEST BETS BUTTON CLICKED      │
                    └─────────────────┬───────────────────┘
                                      │
                                      ▼
                    ┌─────────────────────────────────────┐
                    │    ANALYZE ALL SELECTED TRACKS      │
                    │    (Same scoring as regular flow)   │
                    └─────────────────┬───────────────────┘
                                      │
                                      ▼
                    ┌─────────────────────────────────────┐
                    │         FILTER RESULTS              │
                    │                                     │
                    │ Include only horses with:           │
                    │ • Super Bet (Green border)          │
                    │ • Best Bet (Blue border)            │
                    │ • Good Bet (Purple border)          │
                    └─────────────────┬───────────────────┘
                                      │
                                      ▼
                    ┌─────────────────────────────────────┐
                    │      DISPLAY BY TRACK               │
                    │                                     │
                    │ • Group horses by track name        │
                    │ • Show race number and details      │
                    │ • Maintain clickable functionality  │
                    │ • Color-coded betting indicators    │
                    └─────────────────┬───────────────────┘
                                      │
                        ┌─────────────┴─────────────┐
                        │                           │
                        ▼                           ▼
            ┌─────────────────────┐    ┌─────────────────────┐
            │   SHARE AS TEXT     │    │PROFESSIONAL EXCEL   │
            │                     │    │                     │
            │ • Formatted message │    │ • Real .xlsx format │
            │ • All tracks        │    │ • Auto-color coding │
            │ • All best bets     │    │ • Auto-width columns│
            │ • Outlook/Messages  │    │ • Center alignment  │
            │                     │    │ • Filtering enabled │
            │                     │    │ • Professional style│
            └─────────────────────┘    └─────────────────────┘
```

### 5. Best Bets Filtering Criteria
- **Super Bet (Green)**: Point gap ≥ 8.0 points above second place
- **Best Bet (Blue)**: Point gap 5.0-7.9 points above second place  
- **Good Bet (Purple)**: Point gap 3.0-4.9 points above second place
- **Consider**: Point gap < 3.0 points (excluded from Best Bets)

### 6. Professional Excel Export (BREAKTHROUGH IMPLEMENTATION)
**Technology**: Aspose.Cells for Android via Java (v25.6)
**Output**: Real .xlsx files with complete professional formatting

**Automatic Formatting Applied:**
- **Track Names**: Color-coded backgrounds (orange, yellow, turquoise, pink, gray, lavender)
- **Bet Types**: Color-coded backgrounds (Green=Super Bet, Blue=Best Bet, Purple=Good Bet)
- **Column Alignment**: Auto-centered (Race #, Time, Distance, Horse #, Score, Barrier, Weight)
- **Column Sizing**: Auto-width (Track, Race Name, Horse Name, Jockey, Trainer)
- **Text Features**: Auto-wrapping for long race names
- **Data Features**: Auto-filtering enabled on all columns
- **Professional Styling**: Borders, fonts, and complete professional appearance

**Export Options:**
- **Text Format**: Formatted for easy reading in email/messages
- **Professional Excel**: Real .xlsx with all formatting automatically applied
- **Multi-Track Support**: Single export covers all selected tracks
- **Real-Time Data**: All exports use live analysis results

---

*This flowchart provides a comprehensive visual guide to the complete SteamaTip AI system, including both the detailed scoring methodology and the new Best Bets feature for quick identification of top betting opportunities.*
