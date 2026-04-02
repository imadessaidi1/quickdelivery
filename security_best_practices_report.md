# QuickDelivery Security Best Practices Report

## Executive Summary

The repository currently contains multiple production-grade secrets committed in tracked files, including AWS credentials, database passwords, SMTP credentials, and Keycloak admin credentials. This is the most urgent issue because repository access alone is enough to attempt compromise of cloud resources and core platform services.

The second major issue is an overly permissive gateway CORS policy that allows credentialed cross-origin requests from effectively any HTTP(S) origin if the gateway is reached directly. In addition, the frontend stores both access and refresh tokens in `localStorage`, increasing the impact of any future XSS bug. Finally, the VM1 nginx template does not currently add baseline browser hardening headers such as CSP, `X-Frame-Options`, or `X-Content-Type-Options`.

## Critical Findings

### QD-SEC-001
- Severity: Critical
- Location: [deploy/docker/.env.production](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/docker/.env.production#L5), [deploy/lightsail/vm2-platform.env](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/lightsail/vm2-platform.env#L5), [deploy/lightsail/vm3-app.env](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/lightsail/vm3-app.env#L20), [deploy/lightsail/vm4-app.env](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/lightsail/vm4-app.env#L20), [quickdelivery-googlemaps-front/.env.production](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/quickdelivery-googlemaps-front/.env.production#L4)
- Evidence:
  - [deploy/docker/.env.production](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/docker/.env.production#L5) contains live MySQL, Keycloak, SMTP, Google Maps, MapQuest, and AWS credentials.
  - [deploy/docker/.env.production](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/docker/.env.production#L30) contains an `AWS_ACCESS_KEY_ID`.
  - [deploy/docker/.env.production](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/docker/.env.production#L31) contains an `AWS_SECRET_ACCESS_KEY`.
  - [deploy/lightsail/vm2-platform.env](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/lightsail/vm2-platform.env#L15) contains a live `KEYCLOAK_ADMIN_PASSWORD`.
  - [quickdelivery-googlemaps-front/.env.production](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/quickdelivery-googlemaps-front/.env.production#L4) contains the production browser API key.
- Impact: Anyone with repository access can attempt direct compromise of AWS resources, database access, SMTP abuse, admin console takeover, and third-party quota abuse.
- Fix:
  - Rotate all exposed secrets immediately: AWS, MySQL, Keycloak admin, SMTP, config server credentials, internal gateway token, Maps/MapQuest keys.
  - Purge real secrets from tracked files and keep only templates/examples in git.
  - Move runtime secrets to AWS/Lightsail instance secret distribution or manually provisioned untracked env files.
  - Rewrite git history if the repository has been shared outside a tightly controlled scope.
- Mitigation:
  - Restrict IAM permissions on the exposed AWS key immediately before rotation.
  - Audit CloudTrail, SMTP account activity, Keycloak admin logins, and database access logs for abuse.
- False positive notes:
  - None. These are concrete live-looking credentials in tracked production files.

## High Findings

### QD-SEC-002
- Severity: High
- Location: [config/APIGatewayApplication.yml](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/config/APIGatewayApplication.yml#L42)
- Evidence:
  - [config/APIGatewayApplication.yml](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/config/APIGatewayApplication.yml#L46) allows `http://*:*`
  - [config/APIGatewayApplication.yml](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/config/APIGatewayApplication.yml#L52) allows `https://*:*`
  - [config/APIGatewayApplication.yml](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/config/APIGatewayApplication.yml#L63) sets `allowCredentials: true`
- Impact: If the Spring gateway is reached directly or nginx restrictions are bypassed/misconfigured, arbitrary origins can make credentialed browser requests against privileged APIs.
- Fix:
  - Replace wildcard origin patterns with an explicit allowlist such as `https://app.quickdelivery.fr`, plus tightly scoped local development origins only.
  - Keep production and development CORS settings separate.
  - Reject credentialed wildcard origins entirely.
- Mitigation:
  - Current VM1 nginx CORS restrictions reduce exposure, but this is defense-in-depth only and should not be the only control.
- False positive notes:
  - The current Lightsail firewall and nginx front door may reduce exploitability today, but the application-level configuration remains insecure by default.

### QD-SEC-003
- Severity: High
- Location: [quickdelivery-googlemaps-front/src/config/auth.js](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/quickdelivery-googlemaps-front/src/config/auth.js#L75), [quickdelivery-googlemaps-front/src/config/auth.js](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/quickdelivery-googlemaps-front/src/config/auth.js#L187), [quickdelivery-googlemaps-front/src/config/auth.js](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/quickdelivery-googlemaps-front/src/config/auth.js#L271)
- Evidence:
  - [quickdelivery-googlemaps-front/src/config/auth.js](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/quickdelivery-googlemaps-front/src/config/auth.js#L75) stores OIDC state and verifier in `localStorage`.
  - [quickdelivery-googlemaps-front/src/config/auth.js](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/quickdelivery-googlemaps-front/src/config/auth.js#L188) reads the access token from `localStorage`.
  - [quickdelivery-googlemaps-front/src/config/auth.js](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/quickdelivery-googlemaps-front/src/config/auth.js#L271) stores both access and refresh tokens in `localStorage`.
- Impact: Any future XSS bug or compromised third-party script can exfiltrate bearer tokens and refresh tokens, turning a frontend bug into account takeover.
- Fix:
  - Prefer backend-managed sessions with `HttpOnly` cookies.
  - If bearer tokens must remain browser-side, keep refresh tokens out of persistent storage and keep access tokens in memory only.
  - Reduce third-party script exposure and add stronger browser hardening to reduce XSS blast radius.
- Mitigation:
  - Strong CSP and Trusted Types can reduce the chance of token theft, but they do not fully remove the risk.
- False positive notes:
  - I did not find obvious `v-html` or `innerHTML` sinks in the current frontend, so this is a structural auth-session weakness rather than a demonstrated exploit path.

## Medium Findings

### QD-SEC-004
- Severity: Medium
- Location: [deploy/nginx/quickdelivery.5vm.conf.template](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/nginx/quickdelivery.5vm.conf.template#L37), [deploy/nginx/quickdelivery.5vm.conf.template](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/nginx/quickdelivery.5vm.conf.template#L67), [deploy/nginx/quickdelivery.5vm.conf.template](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/deploy/nginx/quickdelivery.5vm.conf.template#L112)
- Evidence:
  - The VM1 nginx template adds cache and CORS headers, but there are no `add_header` directives for:
    - `Content-Security-Policy`
    - `X-Frame-Options` or CSP `frame-ancestors`
    - `X-Content-Type-Options`
    - `Referrer-Policy`
    - `Permissions-Policy`
- Impact: A frontend XSS or clickjacking issue would have a larger blast radius, and browsers lose several built-in mitigation signals.
- Fix:
  - Add a baseline CSP for the front application.
  - Add `X-Frame-Options: DENY` or a controlled `frame-ancestors` policy.
  - Add `X-Content-Type-Options: nosniff`, a conservative `Referrer-Policy`, and an explicit `Permissions-Policy`.
- Mitigation:
  - If these headers are added elsewhere outside this repo, verify them at runtime and document the source of truth.
- False positive notes:
  - This report is based on the nginx template used for VM1. If CloudFront/CDN/another edge layer injects headers, verify there before treating this as fully exploitable.

### QD-SEC-005
- Severity: Medium
- Location: [config/APIGatewayApplication.yml](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/config/APIGatewayApplication.yml#L6), [config/APIGatewayApplication.yml](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/config/APIGatewayApplication.yml#L96), [config/APIGatewayApplication.yml](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/config/APIGatewayApplication.yml#L136)
- Evidence:
  - [config/APIGatewayApplication.yml](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/config/APIGatewayApplication.yml#L6) uses a default TLS keystore password `QuickDelivery123@`.
  - [config/APIGatewayApplication.yml](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/config/APIGatewayApplication.yml#L96) embeds default Eureka credentials as `http://admin:admin@...`.
  - [config/APIGatewayApplication.yml](C:/Users/imess/Documents/WorkSpace/Projects/DEV_WorkSpace/quickdelivery-parent/config/APIGatewayApplication.yml#L136) falls back to `quickdelivery-internal-token`.
- Impact: Misdeployment or fallback-to-default behavior can silently leave internal trust boundaries protected by known credentials.
- Fix:
  - Remove sensitive default values from application config.
  - Fail fast on startup when required secrets are missing.
  - Replace default internal shared secrets with mandatory env-injected values.
- Mitigation:
  - The current Lightsail hardening reduces public exploitability, but default secrets still weaken internal trust assumptions.
- False positive notes:
  - If production always overrides these values, current exploitability is lower, but the insecure defaults still create a deployment hazard.

## Recommended Remediation Order

1. Rotate all exposed secrets and remove them from tracked files.
2. Lock down gateway CORS to explicit production and development origins only.
3. Plan a migration away from `localStorage` token persistence, starting with removal of refresh-token persistence.
4. Add baseline security headers in the VM1 nginx template.
5. Remove default credentials and shared-secret fallbacks from Spring config.
