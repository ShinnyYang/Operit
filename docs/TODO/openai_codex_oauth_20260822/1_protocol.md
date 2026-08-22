# OAuth And Codex Protocol

## Sources

- https://developers.openai.com/codex/auth.md
- https://developers.openai.com/codex/models.md
- https://github.com/openai/codex/blob/main/codex-rs/login/src/server.rs
- https://github.com/anomalyco/opencode/blob/dev/packages/opencode/src/plugin/openai/codex.ts

## Contract

- Issuer: `https://auth.openai.com`
- Authorization endpoint: `/oauth/authorize`
- Token endpoint: `/oauth/token`
- Revoke endpoint: `/oauth/revoke`
- Codex Responses endpoint: `https://chatgpt.com/backend-api/codex/responses`
- Codex model endpoint: `https://chatgpt.com/backend-api/codex/models`
- OAuth client ID: the public ID used by the current Codex client
- Authorization code exchange: form encoded PKCE request
- Refresh: current Codex token request format, with rotated refresh credentials
- Account routing: `ChatGPT-Account-ID` from the token claims
- Loopback callback: `http://localhost:1455/auth/callback`
- Authorization scope: `openid profile email offline_access`

OAuth access and refresh credentials are never stored in `ModelConfigData`,
exported model backups, request logs, or custom request headers.
