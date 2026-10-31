# KIZUNATUTOR – FRONTEND DEVELOPMENT RULES

**Project:** KizunaTutor – Japanese Language Tutor Matching and Learning Support System

**Purpose:** Establish common frontend development standards to ensure consistent UI, maintainable code, and accurate implementation of the approved Stitch/Figma designs.

---

# 1. TECHNOLOGY STACK

The frontend must follow the technology stack agreed upon by the team.

### Required technologies
- HTML5
- CSS3
- JavaScript ES6+
- Tailwind CSS
- Fetch API for REST API communication

### Restrictions
- Do not introduce React, Vue, Angular, Next.js, or TypeScript unless the team officially changes the frontend stack.
- Do not introduce Bootstrap when Tailwind CSS is the agreed framework.
- Do not mix multiple CSS frameworks without an approved technical reason.
- Avoid unnecessary third-party dependencies.
- Do not introduce a new library without checking whether the project already has an equivalent solution.

**Important:** Follow the existing repository and team conventions. Do not independently change the frontend technology stack.

# 2. DESIGN SYSTEM – BRAND IDENTITY

The approved KizunaTutor Home Page is the primary visual reference for the project's brand identity.

All frontend pages must maintain the same visual language.

### 2.1 Brand direction
- Primary visual identity: Blue.
- Supporting identity: Dark Navy.
- Background style: Warm off-white and cream.
- Accent style: Amber and subtle Japanese red.
- Overall appearance: Professional, trustworthy, friendly, and education-oriented.
- Japanese-inspired elements should be subtle and must not overwhelm the interface.

Do not independently redesign the brand identity.

# 3. COLOR SYSTEM

All pages must use the centralized KizunaTutor color palette.

### 3.1 Brand colors

| Token | HEX | Usage |
|---|---|---|
| Primary | #006194 | Main buttons, links, active elements |
| Primary Hover | #007BB9 | Hover and emphasized states |
| Secondary | #4059AA | Supporting actions and secondary highlights |
| Navy | #0F2545 | Main headings, strong text, dark sections |
| White | #FFFFFF | Cards and content surfaces |

### 3.2 Background colors

| Token | HEX | Usage |
|---|---|---|
| Page Background | #FCFBF7 | Main page background |
| Warm Surface | #FAF8F2 | Inputs, subtle content areas |
| Section Background | #F7F4EC | Alternating page sections |
| White Surface | #FFFFFF | Cards, forms, modals |
| Hero Background | #FAF6EE | Hero section warm gradient start |

### 3.3 Text colors

| Token | HEX | Usage |
|---|---|---|
| Text Primary | #0F2545 | Headings and important content |
| Text Main | #131B2E | Body text |
| Text Secondary | #3F4850 | Descriptions and supporting text |
| Text Muted | #707881 | Labels and less important information |
| Text White | #FFFFFF | Text on dark backgrounds |

### 3.4 Border colors

| Token | HEX | Usage |
|---|---|---|
| Border Default | #E5DECB | Cards and containers |
| Border Light | #ECE8DE | Dividers and subtle boundaries |
| Border Input | #E2D9C6 | Input fields |
| Border Strong | #DED7C5 | Emphasized boundaries |

### 3.5 Accent and status colors

| Token | HEX | Usage |
|---|---|---|
| Amber | #D97706 | Ratings, highlights, Japanese-inspired accents |
| Red | #B91C1C | Subtle Japanese seal or special emphasis |
| Success | #059669 | Successful actions and approved states |
| Warning | #D97706 | Pending or attention-required states |
| Error | #BA1A1A | Errors, rejected states and validation |
| Info | #0284C7 | Informational indicators |

Use status colors consistently. Do not use accent colors as large background areas unless the design requires them.

### 3.6 Color usage rules

- Primary blue must remain the main brand color.
- Navy must be used for important headings and strong text.
- Warm off-white should remain the dominant page background.
- White should be used for cards, forms and content surfaces.
- Amber should be used sparingly for ratings and highlights.
- Red should only be used for small Japanese-inspired accents or appropriate status indicators.
- Green should represent success or positive status, not replace the primary brand color.
- Maintain readable contrast between text and background.
- Do not randomly introduce new HEX colors.
- Do not use arbitrary Tailwind colors when an approved design token already exists.

# 4. CENTRALIZED DESIGN TOKENS

Define colors and common design values centrally.

Use CSS variables in a global stylesheet.

```css
:root {
    /* Brand */
    --color-primary: #006194;
    --color-primary-hover: #007BB9;
    --color-secondary: #4059AA;
    --color-navy: #0F2545;

    /* Background */
    --color-background: #FCFBF7;
    --color-surface: #FFFFFF;
    --color-warm-surface: #FAF8F2;
    --color-section: #F7F4EC;

    /* Text */
    --color-text-primary: #0F2545;
    --color-text-main: #131B2E;
    --color-text-secondary: #3F4850;
    --color-text-muted: #707881;
    --color-text-white: #FFFFFF;

    /* Borders */
    --color-border: #E5DECB;
    --color-border-light: #ECE8DE;
    --color-border-input: #E2D9C6;

    /* Status */
    --color-success: #059669;
    --color-warning: #D97706;
    --color-error: #BA1A1A;
    --color-info: #0284C7;

    /* Accent */
    --color-amber: #D97706;
    --color-japanese-red: #B91C1C;

    /* Typography */
    --font-primary: 'Plus Jakarta Sans', sans-serif;
    --font-japanese: 'Noto Serif JP', serif;

    /* Layout */
    --container-width: 1240px;
    --header-height: 80px;

    /* Radius */
    --radius-sm: 8px;
    --radius-md: 12px;
    --radius-lg: 16px;
    --radius-xl: 24px;
}
```

Keep Tailwind theme colors synchronized with these design tokens.

Do not define duplicate color values independently in every page.

# 5. TYPOGRAPHY RULES

Use the typography from the approved Home Page.

### Primary font
**Plus Jakarta Sans**

Use for:
- Navigation
- Headings
- Body text
- Buttons
- Forms
- Cards
- Labels
- Dashboard content

### Japanese font
**Noto Serif JP**

Use for:
- Japanese characters used as decorative elements
- Kanji watermarks
- Japanese-inspired visual accents

### Typography rules
- Use consistent font weights: 400, 500, 600, 700 and 800.
- Maintain clear heading hierarchy.
- Avoid using too many font sizes on one page.
- Keep body text readable.
- Do not randomly introduce new font families.
- Ensure Vietnamese characters render correctly.
- Avoid using decorative Japanese typography for ordinary Vietnamese content.

# 6. LAYOUT AND SPACING RULES

Follow the layout conventions of the Home Page.

### Main container
- Maximum content width: 1240px.
- Center content horizontally.
- Use consistent horizontal padding.
- Maintain appropriate whitespace between sections.

### Spacing scale

Use consistent spacing values:

- 4px – Extra small
- 8px – Small
- 12px – Medium small
- 16px – Medium
- 24px – Large
- 32px – Extra large
- 40px – Section spacing
- 48px and above – Major section separation

### Border radius

- Small controls: 8px.
- Inputs and buttons: 8–12px.
- Cards: 16px.
- Large feature sections: 24px.
- Circular badges and avatars: 9999px where appropriate.

Do not use random spacing and border-radius values without a design reason.

# 7. UI COMPONENT RULES

Common components must maintain consistent styling throughout the project.

### Header / Navigation
- Follow the approved Home Page navigation design.
- Maintain the same logo position, navigation spacing and action alignment.
- Preserve the approved header height and background.
- Use the correct active navigation state.
- Do not replace the logo with an avatar or use an incorrect brand asset.

### Buttons
- Primary buttons use the official primary color.
- Secondary buttons use the approved secondary style.
- Hover and focus states must be consistent.
- Disabled buttons must be visually distinguishable.
- Maintain consistent height, padding and radius.

### Cards
- Use white or approved surface backgrounds.
- Use consistent border colors and shadows.
- Maintain consistent internal spacing.
- Tutor cards must follow the approved visual language.

### Forms
- Use consistent input heights, borders and focus states.
- Labels must be clear.
- Validation messages must be displayed consistently.
- Required fields must be distinguishable.

### Status badges
Use consistent visual styles for:
- Pending
- Accepted
- Rejected
- Cancelled
- Active
- Inactive
- Verified

Do not invent additional business statuses.

# 8. RESPONSIVE DESIGN

All pages must support:

- Desktop
- Tablet
- Mobile

Requirements:
- Avoid horizontal overflow.
- Navigation must adapt to smaller screens.
- Cards and grids must resize appropriately.
- Forms must remain usable on mobile.
- Text must not overlap images or buttons.
- Maintain consistent spacing at different screen sizes.

Do not implement desktop-only layouts.

# 9. CODE ORGANIZATION

Keep HTML, CSS and JavaScript separated.

Suggested structure:

```text
frontend/
├── index.html
├── pages/
├── assets/
│   ├── images/
│   └── icons/
├── css/
│   ├── global.css
│   ├── layout.css
│   └── custom.css
├── js/
│   ├── config/
│   ├── services/
│   ├── pages/
│   └── utils/
└── README.md
```

Adapt this structure to the existing repository. Do not create duplicate folders unnecessarily.

### Coding conventions
- Use meaningful and consistent variable names.
- Use reusable functions.
- Avoid duplicated code.
- Keep page-specific logic in separate files.
- Avoid putting all JavaScript into one large file.
- Avoid inline JavaScript when a separate script file is appropriate.
- Use semantic HTML elements.
- Add comments for non-obvious business logic.
- Do not remove existing code without authorization.

# 10. TAILWIND CSS RULES

- Use Tailwind CSS for utility-based styling.
- Configure the official KizunaTutor color palette.
- Reuse semantic design tokens.
- Avoid arbitrary HEX values when an official token exists.
- Keep Tailwind configuration consistent with global CSS variables.
- Do not use default Tailwind colors when they conflict with the project palette.
- Use custom CSS for complex or repeated patterns when appropriate.
- Do not mix Tailwind and Bootstrap.

If Tailwind is already configured, preserve the existing setup unless a change is necessary.

# 11. API INTEGRATION RULES

The frontend communicates with the backend through REST APIs.

Expected architecture:

```text
HTML
  ↓
JavaScript
  ↓
Fetch API
  ↓
Spring Boot REST API
  ↓
Service Layer
  ↓
Repository Layer
  ↓
Supabase PostgreSQL
```

Requirements:
- Use Fetch API for HTTP requests.
- Centralize API base URL configuration.
- Separate API services from UI rendering logic.
- Handle loading, success, empty and error states.
- Handle HTTP errors appropriately.
- Do not connect directly to Supabase from browser JavaScript.
- Never expose database credentials or private secrets in frontend files.
- Do not invent API endpoints.
- Confirm actual backend endpoints and request/response structures before integration.
- Follow the backend's authentication mechanism.

# 12. BUSINESS RULES

Frontend implementation must follow the approved KizunaTutor requirements and existing database/business design.

For Learner-side features:

- Learners can discover Japanese tutors.
- Learners can search and filter tutor profiles.
- Learners can view tutor details.
- Learners can submit connection requests.
- Learners can view and track their own connection requests.
- A connection request does not automatically grant access to a Learning Space.
- Learning Space access must follow the actual authorization workflow.
- Do not assume learners make payments.
- Do not invent business statuses, fields or workflows.
- Do not change the agreed use cases without approval.

# 13. SECURITY RULES

- Never expose database passwords, Supabase service-role keys or private secrets in frontend code.
- Do not trust user IDs supplied directly by the browser as proof of identity.
- Follow backend authentication and authorization.
- Validate user input on the frontend for usability, but backend validation remains mandatory.
- Do not store sensitive authentication data in insecure locations.
- Do not bypass backend authorization to make a screen work.

# 14. GIT AND TEAM COLLABORATION

- Work only within the assigned module and files.
- Do not modify another member's module without permission.
- Follow the team's branch naming convention.
- Make commits that describe the actual changes.
- Do not commit `.env` files or secrets.
- Do not overwrite teammates' changes.
- Inspect Git status before modifying shared files.
- Do not merge or push without following the team's agreed workflow.

# 15. ANTIGRAVITY WORKING RULES

Before implementing any task:

1. Read this FRONTEND_RULES.md file.
2. Inspect the existing repository.
3. Identify the assigned page and related files.
4. Inspect the corresponding Stitch/Figma design.
5. Reuse existing components, assets and design tokens.
6. Check whether the required API already exists.
7. Ask for clarification if an essential requirement is missing.

During implementation:

- Work only within the assigned scope.
- Follow the approved design accurately.
- Preserve existing functionality.
- Do not invent business rules.
- Do not refactor unrelated modules.
- Do not modify backend or database files unless explicitly requested.

After implementation:

1. Check the page against the Stitch/Figma reference.
2. Check the official color palette.
3. Check responsive layouts.
4. Check navigation and interactions.
5. Check browser console errors.
6. Report all created and modified files.
7. Report any missing API or unresolved requirements.

# 16. DESIGN REFERENCE PRIORITY

When implementing frontend pages, follow this priority:

1. Approved Stitch/Figma design for the specific screen.
2. Official KizunaTutor Home Page and its Design System.
3. This FRONTEND_RULES.md file.
4. Existing project conventions.

If references conflict, do not silently redesign or change the brand palette. Report the conflict and ask for clarification.

---

**FINAL PRINCIPLE**

Every KizunaTutor frontend page must look and feel like part of the same product.

Maintain consistent colors, typography, navigation, spacing, components, responsive behavior and business logic.

Visual accuracy, code maintainability, team consistency and business-rule correctness are all mandatory.