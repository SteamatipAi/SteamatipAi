# SteamaTip AI - Horse Racing Analysis Scoring Laws

## Professional Documentation & User Guide

---

## Table of Contents
1. [Overview](#overview)
2. [Scoring System Architecture](#scoring-system-architecture)
3. [Detailed Law Explanations](#detailed-law-explanations)
4. [Special Horse Categories](#special-horse-categories)
5. [Scoring Flowchart](#scoring-flowchart)
6. [Technical Implementation](#technical-implementation)
7. [Betting Recommendations](#betting-recommendations)

---

## Overview

SteamaTip AI employs a sophisticated 14-law scoring system that analyzes horses based on multiple performance factors. The system adapts its scoring approach based on the horse's current status (normal racing, returning from spell, or first starter) to ensure fair and accurate predictions.

### Key Principles
- **Real Data Only**: No mock data, hardcoded values, or fallback data
- **Adaptive Scoring**: Different scoring approaches for different horse categories
- **Comprehensive Analysis**: 14 distinct laws covering all aspects of horse performance
- **Transparent Scoring**: Every point is explained and traceable
- **Granular Track/Distance Analysis**: Separate scoring for distance, track, and combined performance

---

## Scoring System Architecture

### Total Points Available
- **Normal Horses**: 127 points (14 active laws)
- **Spell Horses**: 127 points (14 active laws including 2nd up recent form bonus)
- **First Starters**: 58 points (7 laws - limited historical data)

### Horse Categories
1. **Normal Horses**: Regular racing form with recent starts
2. **Spell Horses**: Returning from 12+ week break (1st Up or 2nd Up)
3. **First Starters**: Horses with no previous race history

---

## Detailed Law Explanations

### For Normal Horses (14 Laws - 127 Points Total)

#### Law 1: Recent Form (25 Points)
**Purpose**: Evaluates the horse's performance in its last 5 races

**Scoring Method**:
- **Position Scoring**: Win (5 pts), 2nd/3rd (3 pts), 4th/5th (1.5 pts), 6th-8th (1 pt)
- **Recency Weighting**: More recent races receive higher multipliers (5x, 4x, 3x, 2x, 1x)
- **Last Start Bonus**: +3 points if finished within 4 lengths of winner (regardless of position)
- **Maximum**: 25 points

**Example**: A horse with form "12145" would score highly due to recent wins and good finishes.

#### Law 2: Class Suitability (25 Points)
**Purpose**: Assesses how the current race class compares to the horse's recent races

**Scoring Method**:
- **Class Drop Bonus**: +15 points (dropping in class)
- **Class Drop + Good Last Start**: +20 points total
- **Declining Horse Detection**: -5 points penalty for 3+ consecutive class drops (indicates decline, not tactical drop)
- **Similar Class**: +3 points per good performance (1st-3rd) in similar classes
- **Class Rise**: +10 points if has form in lower classes, -5 points penalty if no lower class form
- **Maximum**: 25 points

#### Law 3: Distance Success (8 Points)
**Purpose**: Awards points for winning or placing at this distance previously

**Scoring Method**:
- **Win Rate Bonus** (0-4 points): Based on win rate at this distance
- **Place Rate Bonus** (0-4 points): Based on place rate at this distance
- **Maximum**: 8 points

**Note**: This law focuses ONLY on distance performance, regardless of track.

#### Law 4: Track Success (8 Points)
**Purpose**: Awards points for winning or placing at this track previously

**Scoring Method**:
- **Win Rate Bonus** (0-4 points): Based on win rate at this track
- **Place Rate Bonus** (0-4 points): Based on place rate at this track
- **Maximum**: 8 points

**Note**: This law focuses ONLY on track performance, regardless of distance.

#### Law 5: Track+Distance Combined (9 Points)
**Purpose**: Awards points for winning or placing at this specific track AND distance combination

**Scoring Method**:
- **Combined Win Rate** (0-2 points): Based on wins at this exact track+distance
- **Combined Place Rate** (0-2 points): Based on places at this exact track+distance
- **Distance Pattern Bonus** (0-5 points): Rewards horses staying within comfort zone (±200m) or having won at this exact distance before
- **Maximum**: 9 points

**Note**: This law only awards points if the horse has actually run at this specific track+distance combination before. The pattern bonus rewards consistency in distance racing.

#### Law 6: Sectional Time (8 Points)
**Purpose**: Rewards horses with fast finishing speeds from their last start

**Scoring Method**:
- **≤33.0s**: 8 points (Very fast finishing)
- **≤34.0s**: 6 points (Fast finishing)
- **≤35.0s**: 4 points (Good finishing)
- **≤36.0s**: 2 points (Average finishing)
- **>36.0s**: 0 points (Slow finishing)

#### Law 7: Barrier Position (6 Points) - Distance-Aware
**Purpose**: Favors horses drawn in inside barriers, with scoring adjusted for race distance

**Scoring Method**:
- **Short Races (≤1200m)**:
  - Barriers 1-4: 6 points (Inside barriers crucial)
  - Barriers 5-8: 3 points (Mid barriers acceptable)
  - Barriers 9+: 0 points (Wide barriers disadvantaged)
- **Medium/Long Races (>1200m)**:
  - Barriers 1-6: 4 points (Inside barriers helpful)
  - Barriers 7-12: 2 points (Mid barriers acceptable)
  - Barriers 13+: 0 points (Wide barriers disadvantaged)

**Note**: Barrier advantage is more critical in short races with tight turns.

#### Law 8: Jockey Performance (8 Points)
**Purpose**: Evaluates the jockey's current premiership ranking

**Scoring Method**:
- **Champion Jockeys**: 8 points (Craig Williams and other designated champions always receive maximum points)
- **Rank 1-5**: 8 points
- **Rank 6-10**: 5 points
- **Rank 11-20**: 2 points
- **Rank 21+**: 0 points

**Champion Jockey Override**: Elite jockeys like Craig Williams receive maximum points regardless of current premiership standing, ensuring quality riders are properly recognized.

#### Law 9: Trainer Performance (8 Points)
**Purpose**: Evaluates the trainer's current premiership ranking

**Scoring Method**:
- **Rank 1-5**: 8 points
- **Rank 6-10**: 5 points
- **Rank 11-20**: 2 points
- **Rank 21+**: 0 points

#### Law 10: Jockey-Horse Relationship (8 Points)
**Purpose**: Rewards successful partnerships between jockey and horse

**Scoring Method**:
- **2+ Wins Together**: 4 points
- **1 Win Together**: 2 points
- **2+ Places Together**: 1 point
- **1 Place Together**: 0.5 points
- **No Success Together**: 0 points

#### Law 11: Track Condition (8 Points)
**Purpose**: Assesses the horse's success on similar track conditions

**Scoring Method**:
- **Win on Similar Condition**: 8 points
- **2nd on Similar Condition**: 5 points
- **3rd on Similar Condition**: 3 points
- **Other Results**: 0 points
- **Condition Categories**: Firm/Good, Soft, Heavy, Synthetic

#### Law 12: Weight Advantage (8 Points)
**Purpose**: Compares horse's weight to field average, rewarding those carrying less weight

**Scoring Method**:
- **4+ kg below average**: 8 points (Significant advantage)
- **2-3.9 kg below average**: 5 points (Good advantage)
- **0-1.9 kg below average**: 2 points (Small advantage)
- **At or above average**: 0 points (No advantage)
- **Maximum**: 8 points

**Note**: Weight handicapping is a critical factor in Australian racing. Horses carrying less weight than the field average have a measurable advantage.

#### Law 13: Freshness (3 Points)
**Purpose**: Awards bonus for optimal time between runs

**Scoring Method**:
- **14-28 days**: 3 points (Ideal freshness)
- **29-56 days**: 1 point (Acceptable freshness)
- **<14 days**: 0 points (Backing up too quickly)
- **>56 days**: 0 points (Too long between runs for non-spell horses)
- **Maximum**: 3 points

**Note**: Spell horses (12+ weeks) don't receive freshness points as they're already categorized as fresh from spell.

---

### For Spell Horses (14 Laws - 127 Points Total)

Spell horses (returning from 12+ week break) use a modified scoring system with spell-specific laws:

#### Law 1: 1st Up Performance (8 Points) - ACTIVE for 1st Up horses
**Purpose**: Evaluates the horse's historical performance in first starts after spells

**Scoring Method**:
- **Ever Won 1st Up**: +5 points
- **Ever Placed 1st Up** (2nd or 3rd): +3 points
- **Maximum**: 8 points

#### Law 2: 2nd Up Performance (8 Points) - ACTIVE for 2nd Up horses
**Purpose**: Evaluates the horse's historical performance in second starts after spells

**Scoring Method**:
- **2nd Up Wins**: +5 points
- **2nd Up Seconds**: +3 points
- **2nd Up Thirds**: +1 point
- **Maximum**: 8 points

#### Law 3: 2nd Up Recent Form Bonus (8 Points) - ACTIVE for 2nd Up horses
**Purpose**: Awards bonus points for first-up performance when horse is second up (fixes scoring anomaly)

**Scoring Method**:
- **1st place first-up**: +8 points
- **2nd place first-up**: +5 points
- **3rd place first-up**: +3 points
- **4th place first-up**: +2 points
- **Competitive effort bonus**: +1 point (within 4 lengths)
- **Maximum**: 8 points

**Note**: This addresses the issue where second up horses received 0 for recent form despite good first-up runs.

#### Laws 4-14: Same as Normal Horses (102 points total)
- Law 4: Class Suitability (25 pts)
- Law 5: Distance Success (8 pts)
- Law 6: Track Success (8 pts)
- Law 7: Track+Distance Combined (9 pts)
- Law 8: Sectional Time (8 pts) - Only for 2nd Up horses
- Law 9: Barrier (6 pts) - Distance-aware
- Law 10: Jockey (8 pts)
- Law 11: Trainer (8 pts)
- Law 12: Jockey-Horse Relationship (8 pts)
- Law 13: Track Condition (8 pts)
- Law 14: Weight Advantage (8 pts)
- Law 15: Freshness (0 pts for spell horses - exempt as already fresh from spell)

---

### For First Starters (7 Laws - 58 Points Maximum)

First starters have limited scoring due to no race history:

#### Law 1: Jockey Performance (8 Points)
#### Law 2: Trainer Performance (8 Points)
#### Law 3: Barrier Position (6 Points) - Distance-aware
#### Law 4: Trial Sectional Times (8 Points)
- **≤33.0s**: 8 points (Very fast trial)
- **≤34.0s**: 6 points (Fast trial)
- **≤35.0s**: 4 points (Good trial)
- **≤36.0s**: 2 points (Average trial)
- **>36.0s**: 0 points (Slow trial)

#### Law 5: Jockey-Horse Relationship (0 Points) - No history available
#### Law 6: Weight Advantage (8 Points)
#### Law 7: Freshness (0 Points) - No race history available

---

## Special Horse Categories

### Spell Detection Logic
The system detects spell horses by analyzing the form string right-to-left:

- **Form String**: "32x1x2212x"
- **Reading Right-to-Left**: x, 2, 1, 2, x, 1, x, 2, 3
- **1st Up**: Most recent character is 'x' (spell)
- **2nd Up**: Pattern is "number + x" (e.g., "2x" = 2nd place first up, now 2nd up)

### Scratched Horses
- **Handling**: Scratched horses are automatically excluded from scoring
- **Display**: Show "SCR" status with no score
- **No Impact**: Other horses' scores remain unchanged

---

## Scoring Flowchart

```
START: Horse Analysis
    │
    ▼
Has Real Form Data?
    │
    ├─ NO ──► EXCLUDE (No Score)
    │
    └─ YES ──► Check Horse Category
        │
        ├─ FIRST STARTER ──► Apply First Starter Laws (7 laws, 58 pts max)
        │   │
        │   └─ Laws: Jockey, Trainer, Barrier, Trial Sectionals, Weight Advantage
        │
        ├─ SPELL HORSE ──► Check Spell Status
        │   │
        │   ├─ 1ST UP ──► Apply Spell Horse Laws (14 laws, 127 pts max)
        │   │   │
        │   │   └─ Laws: 1st Up (ACTIVE), 2nd Up (inactive), 2nd Up Bonus (inactive),
        │   │       Class, Distance, Track, Track+Distance, Sectional (inactive), Barrier,
        │   │       Jockey, Trainer, Jockey-Horse, Track Condition, Weight, Freshness (0)
        │   │
        │   └─ 2ND UP ──► Apply Spell Horse Laws (14 laws, 127 pts max)
        │       │
        │       └─ Laws: 1st Up (inactive), 2nd Up (ACTIVE), 2nd Up Bonus (ACTIVE),
        │           Class, Distance, Track, Track+Distance, Sectional (ACTIVE), Barrier,
        │           Jockey, Trainer, Jockey-Horse, Track Condition, Weight, Freshness (0)
        │
        └─ NORMAL HORSE ──► Apply Normal Horse Laws (14 laws, 127 pts max)
            │
            └─ Laws: Recent Form, Class, Distance, Track, Track+Distance, Sectional,
                Barrier, Jockey, Trainer, Jockey-Horse, Track Condition, Weight, Freshness
    │
    ▼
Calculate Total Score
    │
    ▼
Apply Tie Resolution (Win/Place Ratio)
    │
    ▼
Generate Betting Recommendation
    │
    ▼
END: Display Results
```

---

## Technical Implementation

### Data Sources
- **Racing Australia**: Official race results and form data
- **Premiership Tables**: Current jockey and trainer rankings
- **Real-time Scraping**: Live data extraction from official sources

### Scoring Engine Features
- **Deterministic Scoring**: Same inputs always produce same outputs
- **No Random Elements**: All scoring is based on historical data
- **Transparent Calculations**: Every point is logged and traceable
- **Real Data Validation**: Only horses with complete real data are scored

### Performance Optimization
- **Parallel Processing**: Multiple tracks analyzed simultaneously (75% faster)
- **Smart Caching**: Premiership data cached by state to eliminate duplicate fetches
- **Efficient Parsing**: Optimized HTML parsing for form data extraction
- **Error Handling**: Graceful handling of missing or invalid data
- **Scaling Factors**: Horses with <5 races get proportional scoring for fairness

---

## Betting Recommendations

### Recommendation Logic
Betting recommendations are based on the point gap between 1st and 2nd place horses:

- **Super Bet** (8+ point gap): Highest confidence - significant advantage
- **Best Bet** (5-7.9 point gap): High confidence - clear advantage  
- **Good Bet** (3-4.9 point gap): Moderate confidence - some advantage
- **Consider** (<3 point gap): Standard analysis - minimal advantage

### Visual Indicators
- **Super Bet**: Special highlighting with highest confidence
- **Best Bet**: Enhanced highlighting with high confidence
- **Good Bet**: Moderate highlighting with some confidence
- **Consider**: Standard gold border with no special highlighting

---

## Best Bets Feature

### Overview
The Best Bets feature provides a streamlined analysis focused exclusively on horses with the strongest betting recommendations. This feature filters the complete analysis to show only horses that meet specific confidence thresholds.

### Best Bets Criteria
Only horses meeting these betting recommendation levels are included:

#### Super Bet (Green Border)
- **Point Gap**: ≥ 8.0 points above second place
- **Confidence**: Highest - significant scoring advantage
- **Visual Indicator**: Bright green border
- **Recommendation**: Strong betting opportunity

#### Best Bet (Blue Border)  
- **Point Gap**: 5.0-7.9 points above second place
- **Confidence**: High - clear scoring advantage
- **Visual Indicator**: Blue border
- **Recommendation**: Good betting opportunity

#### Good Bet (Purple Border)
- **Point Gap**: 3.0-4.9 points above second place  
- **Confidence**: Moderate - noticeable scoring advantage
- **Visual Indicator**: Purple border
- **Recommendation**: Reasonable betting opportunity

### Best Bets User Flow
1. **Multi-Track Selection**: Select multiple tracks using existing checkbox system
2. **Click Best Bets**: Use dedicated Best Bets button (green border)
3. **Filtered Analysis**: System analyzes all tracks but shows only qualifying horses
4. **Track Organization**: Results grouped by track with clear headers
5. **Export Options**: Share as formatted text or export to Excel (CSV)

### Export Functionality
- **Text Format**: Clean, formatted message suitable for email/messaging
- **Professional Excel Export**: Real .xlsx files with complete formatting using Aspose.Cells for Android:
  - **Automatic color coding**: Track names (orange, yellow, turquoise, pink, gray, lavender) and bet types (Green=Super Bet, Blue=Best Bet, Purple=Good Bet)
  - **Auto-sized columns**: Track names, race names, horse names, jockey/trainer names automatically sized
  - **Center alignment**: Race numbers, time, distance, horse numbers, scores, barriers, weights perfectly centered
  - **Text wrapping**: Race names wrap automatically for long titles
  - **Auto-filtering**: All data rows include filtering for advanced analysis
  - **Professional styling**: Complete borders, fonts, and professional appearance
- **Multi-Track Support**: Single export covers all selected tracks with organized layout
- **Data Integrity**: All scores, recommendations, and details accurately preserved

### Data Integrity
The Best Bets feature maintains the same strict data integrity standards:
- **Real Data Only**: No mock or fallback data
- **Same Scoring Laws**: Uses identical scoring methodology as regular analysis
- **Live Analysis**: Fresh analysis performed for each Best Bets request
- **Transparent Filtering**: Clear criteria for inclusion/exclusion

## Conclusion

SteamaTip AI's scoring system provides a comprehensive, fair, and transparent method for analyzing horse racing performance. The adaptive approach ensures that horses are evaluated based on their specific circumstances, whether they're regular runners, returning from spells, or first-time starters.

The addition of the Best Bets feature provides users with a powerful tool for quickly identifying the strongest betting opportunities across multiple tracks, while maintaining the same rigorous analysis standards.

The system's reliance on real data and transparent calculations ensures that users can trust the results and understand exactly why each horse received its score. This professional approach to horse racing analysis sets SteamaTip AI apart as a reliable and sophisticated betting analysis tool.

---

*Document Version: 2.3*  
*Last Updated: October 2025*  
*SteamaTip AI - Professional Horse Racing Analysis with 14-Law System, Granular Track/Distance Analysis, and Distance-Aware Barrier Scoring*
