# ADR 0001: Where tokens are stored

Status: Accepted, 11 Nov 2026

## Decision
- Access token: kept in browser memory only, not in localStorage.
- Refresh token: HttpOnly, Secure, SameSite=Strict cookie (added in Module 1, Phase 2).

## Reason
JavaScript cannot read HttpOnly cookies, and an in-memory token disappears when the tab closes.
Both limit the damage from a cross-site scripting (XSS) attack.
