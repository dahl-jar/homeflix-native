<p align="center">
  <img src="apps/ios/assets/icon.png" width="112" alt="Homeflix">
</p>

<h1 align="center">Homeflix</h1>

<p align="center">
  A sideloaded iOS frontend for a self-hosted media library.
</p>

> Homeflix depends on custom Jellyfin API extensions from the Homeflix server. It may not work with a stock Jellyfin install.

## Showcase

### Home

Recommendations, recently added media, and in-progress titles from the connected library.

<p align="center">
  <img src="docs/images/home.png" width="42%" alt="Home screen">
</p>

### Library

The movie library supports sorting and filters for genre, decade, rating, and watch status.

<p align="center">
  <img src="docs/images/library.png" width="42%" alt="Movie library">
</p>

### Movie details

Details combine metadata, playback actions, an overview, genres, and related titles.

<p align="center">
  <img src="docs/images/movie-detail.png" width="42%" alt="Movie details">
</p>

### Playback pipeline

Play starts a server-driven pipeline that checks sources, analyzes the selected source, chooses tracks, prepares the stream, and hands it to the native player.

<p align="center">
  <img src="docs/images/pipeline.png" width="42%" alt="Playback pipeline">
</p>

### Player

The native player handles seeking, play and pause, screen fit, control locking, audio, subtitles, and next-episode playback.

<p align="center">
  <img src="docs/images/player.png" width="92%" alt="Landscape player with controls visible">
</p>

### Episodes

The episode picker shows the current and following episodes with artwork, runtime, and summaries without leaving playback.

<p align="center">
  <img src="docs/images/episodes.png" width="92%" alt="Landscape episode selector">
</p>

## Run

Create the ignored local configuration file, then add one or more comma-separated server URLs to `EXPO_PUBLIC_HOMEFLIX_SERVER_URLS`.

```bash
cp apps/ios/.env.example apps/ios/.env.local
pnpm install
pnpm ios

pnpm check
```

## Sideload

The build workflow produces an unsigned `Homeflix.ipa` from `main`. Sign and install it with an iOS sideloading tool. Local build steps are in the [iOS README](apps/ios/README.md).
