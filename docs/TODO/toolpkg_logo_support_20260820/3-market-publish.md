# Market Publish

## Existing

Artifact publication registers JSON metadata and a release asset. The market
entry model has no logo field, and this repository does not contain the market
Worker implementation.

## Change

- Extract the declared ToolPkg logo from the local archive during publication.
- Upload it through an authenticated market logo endpoint.
- Include the returned hosted logo reference in publish, metadata update and
  new-version requests.
- Decode the optional logo URL in static market entries.
- Keep old entries valid when the field is absent.

The Android client contract is `POST /market/v2/logos` with a multipart field
named `logo`. The response is `{ "ok": true, "url": "https://..." }`. Publish
and update requests carry the returned URL as the optional `logoUrl` field.

The Worker/API implementation is outside this Android repository and must expose
this contract before production publishing is enabled.

[DONE]

The API endpoint and static market generator must be implemented or coordinated
outside this Android worktree before the full publish flow can be exercised.
