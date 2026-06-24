# ISBN Book Sorter Design System

## 1. Atmosphere & Identity

A quiet cataloging tool for a personal or office library. The signature is a ledger-like mobile workspace: restrained surfaces, clear scan state, and compact grouped book records that make repeated ISBN entry feel calm and reliable.

## 2. Color

### Palette

| Role | Token | Light | Dark | Usage |
|------|-------|-------|------|-------|
| Surface/primary | --surface-primary | #F7F5F0 | #12110F | App background |
| Surface/secondary | --surface-secondary | #FFFFFF | #1C1A17 | Panels and form groups |
| Surface/elevated | --surface-elevated | #FAFAF8 | #24211D | Scanner and result surfaces |
| Text/primary | --text-primary | #1F2521 | #F4F1EA | Main labels and book titles |
| Text/secondary | --text-secondary | #667069 | #B7B0A5 | Metadata and helper text |
| Text/tertiary | --text-tertiary | #929A94 | #7B7368 | Disabled and quiet captions |
| Border/default | --border-default | #D9D5CA | #38332C | Dividers and inputs |
| Border/subtle | --border-subtle | #E9E4D8 | #2B2722 | Soft separators |
| Accent/primary | --accent-primary | #2F6F4E | #6FC18C | Primary actions and focus |
| Accent/hover | --accent-hover | #255A40 | #83D99F | Pressed action states |
| Status/success | --status-success | #2F6F4E | #6FC18C | Saved or found state |
| Status/warning | --status-warning | #A36B1F | #D8A348 | Missing key or fallback state |
| Status/error | --status-error | #B84A3A | #EA7565 | Lookup and permission errors |
| Status/info | --status-info | #426E8A | #7DB1D1 | Scan guidance |

### Rules

- Accent appears only on controls and actionable state.
- Warnings identify fallback behavior without looking destructive.
- Scanner surfaces use tonal shift instead of heavy decoration.

## 3. Typography

### Scale

| Level | Size | Weight | Line Height | Tracking | Usage |
|-------|------|--------|-------------|----------|-------|
| H1 | 24sp | 700 | 1.2 | 0 | App title |
| H2 | 18sp | 700 | 1.3 | 0 | Category headers |
| H3 | 16sp | 700 | 1.35 | 0 | Book titles |
| Body | 15sp | 400 | 1.5 | 0 | Default UI text |
| Body/sm | 13sp | 400 | 1.45 | 0 | Metadata and helper text |
| Caption | 12sp | 500 | 1.35 | 0 | Status chips and labels |

### Font Stack

- Primary: Android system sans-serif.
- Mono: Android system monospace for ISBN values only.

### Rules

- Body text never appears below 13sp.
- ISBN strings use monospace to improve scanning and comparison.

## 4. Spacing & Layout

### Base Unit

All spacing derives from a base of 4dp.

| Token | Value | Usage |
|-------|-------|-------|
| --space-1 | 4dp | Tight inline gaps |
| --space-2 | 8dp | Compact lists |
| --space-3 | 12dp | Input padding |
| --space-4 | 16dp | Section padding |
| --space-5 | 20dp | Scanner spacing |
| --space-6 | 24dp | Page top and bottom rhythm |

### Grid

- Single-column mobile layout.
- Header controls start below the status bar safe area; the menu trigger sits at the upper-left before the title.
- Scanner preview uses a stable 4:3 frame.
- Grouped book records are full width with fixed internal rhythm.

### Rules

- Use multiples of 4dp only.
- Controls keep a minimum touch target of 48dp.

## 5. Components

### Scanner Panel
- **Structure**: preview frame, scan action row, status text.
- **Variants**: ready, scanning, paused, error.
- **Spacing**: --space-4 and --space-5.
- **States**: disabled when camera permission is missing.
- **Accessibility**: buttons use explicit Korean labels.
- **Motion**: none beyond native press feedback.

### Book Group
- **Structure**: category heading with count, repeated book rows.
- **Variants**: populated, empty.
- **Spacing**: --space-3 and --space-4.
- **States**: empty state explains how to add the first book; populated rows are clickable and open a full bibliographic detail view.
- **Accessibility**: title, author, publisher, ISBN, and source are visible text.
- **Motion**: none.

### Book Detail Page
- **Structure**: full-page, scrollable label/value bibliography fields with a single return-to-library action.
- **Variants**: complete metadata, missing fields shown as "정보 없음".
- **Spacing**: --space-2 and --space-3.
- **States**: opened from a saved book row.
- **Accessibility**: title, subtitle, author, translator, publisher, publication date, category, ISBN, source, language, price, form, size, series, related ISBN, National Library title URL/EA ISBN/add code/input date/update date/status flags, page count, cover URL, saved date, description, introduction, contents, and table of contents are visible text; ISBN keeps monospace styling.
- **Motion**: page transition by immediate content replacement.

## 6. Motion & Interaction

### Timing

| Type | Duration | Easing | Usage |
|------|----------|--------|-------|
| Micro | 100ms | ease-out | Native button press |
| Standard | 200ms | ease-in-out | Status text changes |

### Rules

- Keep scanning interactions direct and predictable.
- No layout-shifting animation while the camera is active.

## 7. Depth & Surface

### Strategy

borders-only

| Type | Value | Usage |
|------|-------|-------|
| Default | 1dp solid var(--border-default) | Inputs, scanner frame, book rows |
| Subtle | 1dp solid var(--border-subtle) | Group separators |
