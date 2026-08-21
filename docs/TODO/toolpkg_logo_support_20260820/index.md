---
fork: https://github.com/AAswordman/Operit.git
status: complete
---

# ToolPkg Logo Support

## Current State

ToolPkg containers do not expose a package logo. The package manager renders the
generic Apps icon, while the market list and detail header render a title-derived
avatar. The model provider logo loader already contains AndroidSVG and bitmap
scaling code, but it is tied to APK assets and provider identifiers.

## Intent

Add an optional package logo resource that travels inside a ToolPkg archive. The
same resource is rendered locally from the package cache and is uploaded to the
market as a separately hosted image for pre-install market screens.

Existing ToolPkg archives and existing market entries remain valid. The new
manifest field is optional and `schema_version` remains unchanged.

## Manifest Contract

```json
{
  "logo": "plugin_logo",
  "resources": [
    {
      "key": "plugin_logo",
      "path": "resources/logo.svg",
      "mime": "image/svg+xml"
    }
  ]
}
```

`logo` is a resource key. Supported static formats are SVG, PNG, JPEG and WebP.

## Scope

- ToolPkg manifest parsing, cache access and package-manager rendering
- Generic SVG and bitmap logo rendering shared with provider logos
- Artifact publication logo extraction and market-hosted upload
- Market list and detail rendering
- ToolPkg format documentation and a small example resource
- Static review only in this change; no build or test command is run by default
- The ToolPkg publish screen accepts a local SVG or bitmap logo, previews the selected
  image, and writes it only into the temporary direct-upload archive when needed.
- The publish screen also previews the logo in the real market list-card and detail-header
  layouts without uploading or publishing the artifact.

Android client implementation is complete. The market Worker/API remains an
external dependency because it is not part of this repository.
