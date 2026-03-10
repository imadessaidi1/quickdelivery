# QuickDelivery Premium UX/UI Specification

## 1. Product Goal
Design a premium, reliable, and fast local delivery experience with 3 user roles:
1. `ROLE_CLIENT` (create and track packages)
2. `ROLE_LIVREUR` (find, reserve, pick up, and deliver packages)
3. `ROLE_ADMIN` (validate accounts and manage operations)

## 2. Premium UX Principles
1. Perceived speed: instant feedback, skeleton loaders, short transitions.
2. Decision clarity: one primary action per screen.
3. Controlled density: readable map and strong visual hierarchy.
4. Trust: explicit statuses, confirmations, and traceability.
5. Accessibility: AA contrast, visible keyboard focus, explicit labels.

## 3. Global Navigation Structure
1. Fixed top bar on desktop/tablet, simplified on mobile.
2. Role-based dynamic menu.
3. Post-login role redirection:
   1. `ROLE_ADMIN` -> `/usersAccountValidation`
   2. `ROLE_CLIENT` -> `/createPackage`
   3. `ROLE_LIVREUR` -> `/`
4. Address search bar shown only on `Map` and `My Packages` screens.

## 4. Role-based Menus
1. Client: `Create Package`, `My Packages`, `Account` (My Account, Logout).
2. Driver: `Home/Map`, `My Packages`, `Account` (My Account, Logout).
3. Admin: full menu access (including account validation).
4. Menu states: default, active, hover, disabled, loading.

## 5. Main User Flows
1. Email signup flow:
   1. Sign up -> email verification -> account activation -> login -> role redirect.
2. Social login flow (Google/Facebook):
   1. OAuth login -> app callback -> account link/create -> role assignment -> role redirect.
3. Client flow:
   1. Search address -> view nearby packages -> create package -> track status.
4. Driver flow:
   1. Open map -> view nearby packages -> select package -> review route -> reserve -> pickup -> deliver.
5. Admin flow:
   1. Login -> pending users list -> approve/reject -> quick audit.

## 6. Screen Specifications
1. `Login / Auth Landing`
   1. Goal: clear entry point for email and social login.
   2. Content: login form, social CTAs, legal links, trust message.
   3. States: invalid credentials, unverified account, blocked account, loading.
2. `Email Validation`
   1. Goal: confirm activation with clear feedback.
   2. Content: success/failure message, `Go to Login` CTA.
   3. States: expired token, already verified.
3. `Driver Home Map`
   1. Goal: fast discovery and action.
   2. Content: full map, package carousel, selected package details, address search.
   3. Behavior: selected package highlighted, other markers dimmed, start/end markers (A/B) always visible.
   4. Responsive: vertical left carousel on desktop/tablet, horizontal on mobile.
4. `Create Package (Client)`
   1. Goal: complete shipment creation in under 2 minutes.
   2. Content: pickup/dropoff addresses, dimensions/weight, price, time slot.
   3. UX: inline validation, cost/time summary.
5. `My Packages` (Client/Driver)
   1. Goal: track packages and show role-based actions.
   2. Content: filterable list, status timeline, contextual actions.
   3. Address search available (as requested).
6. `Package Details`
   1. Goal: complete package view and timeline.
   2. Content: package info, addresses, status, current actor, OTP section when relevant.
7. `User Validation (Admin)`
   1. Goal: process account requests quickly.
   2. Content: filterable table, user details panel, approve/reject actions.
   3. UX: mandatory confirmation, rejection reason required.
8. `My Account`
   1. Goal: manage profile and security settings.
   2. Content: personal data, preferences, security, active session info.
9. `Error Screens`
   1. 401: not authenticated -> `Login` CTA.
   2. 403: forbidden -> required role explanation.
   3. 404/500: clear message + useful next action.

## 7. Functional Rules to Make Explicit in UI
1. Admin self-signup is forbidden.
2. A client can become a driver (explicit process).
3. `ROLE_CLIENT_PRO` can access bulk package features.
4. Backend endpoints are accessible only through the gateway.
5. Session expiration triggers clean auth redirect with context recovery.

## 8. Required UI States
1. Empty state (no package found).
2. Loading state (list/map/form).
3. Success state (action confirmed).
4. Error state (technical issue + recovery action).
5. Offline/degraded network state (where relevant).

## 9. Design System Components to Mock
1. Top bar, side/mobile navigation.
2. Address search bar (with autocomplete).
3. Package cards (compact and expanded).
4. Package carousel (horizontal and vertical variants).
5. Buttons: primary, secondary, ghost, danger.
6. Inputs: text, select, autocomplete, date.
7. Toast, alert, modal.
8. Admin table.
9. Package status timeline.
10. Status/role badges.

## 10. Expected Premium Visual Direction
1. Inspiration: Uber/Uber Eats style clarity, strong hierarchy, clean surfaces.
2. Typography: modern and highly readable.
3. Colors: brand-first palette with coherent status colors (success/warning/error).
4. Motion: subtle micro-interactions for feedback.
5. Iconography: simple, consistent, low noise.

## 11. Deliverables Required from UX/UI Expert
1. Clickable user flows by role.
2. Low-fidelity wireframes for all screens.
3. High-fidelity UI for desktop/tablet/mobile.
4. Interactive prototype with states and transitions.
5. Design tokens (color, type, spacing, radius, shadows).
6. Developer handoff specs (components, behaviors, edge cases).

## 12. UX Acceptance Criteria
1. Each role reaches its primary goal in 3 actions or less.
2. Post-login redirection works by role.
3. No screen is missing loading/empty/error states.
4. Navigation and search are usable on mobile and desktop.
5. Premium visual consistency is maintained across the app.
