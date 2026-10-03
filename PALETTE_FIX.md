# Palette visibility fix

The previous production UI stored the accent palette correctly and exposed it inside Settings > Appearance, but the primary header control only toggled Light/Dark. This made the palette feature easy to miss.

This revision adds a dedicated **Theme & Colors** control directly to the main header and sidebar. It exposes:

- Light / Dark / System
- 12 accent palettes
- Current palette indicator
- Immediate palette switching
- Persistent selection through the existing Zustand theme store

The existing Settings > Appearance palette grid remains available as the full-size appearance editor.
