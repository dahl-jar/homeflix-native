# Player controls and iOS Simulator

The player uses the custom React Native controls under `src/playback/`. The iOS
Simulator runs the same playback pipeline, track selectors, orientation rules,
and Jellyfin reporting used by a device build.

## Run the development build

Playback depends on native Expo modules, so use a development build:

```sh
cd homeflix-native
pnpm install
pnpm exec expo run:ios --device "iPhone 17 Pro Max"
```

Launch an installed build again without rebuilding it:

```sh
xcrun simctl launch booted dev.dahl-jar.homeflix-native
```

The player route permits rotation. Every other route locks to portrait. Closing
the player stops the playback session, restores portrait, and returns to the
previous route. If no previous route exists, it opens Home.

## Pipeline handoff

Tapping Play opens the player route with the item ID and resume position. The
poster remains visible while the server-owned pipeline discovers and checks
sources. The progress rail renders the stage IDs, labels, order, and outcomes
returned by the API. A new server stage appears without a matching client-side
stage definition.

Player controls stay unavailable until the server accepts a source and releases
a signed playback URL. The video surface then starts one native player. The
poster and pipeline overlay close after the native player reports playback or
its media clock advances.

## On-screen controls

Tap the video surface to show or hide the controls. Controls remain visible
while paused and hide 3.5 seconds after playback resumes.

The top row contains:

- **Back**: stops playback, restores portrait, and leaves the player.
- **Fill screen**: changes the video from `contain` to `cover`. **Fit video**
  changes it back to `contain`. The custom player stays active for both modes.
- **Lock controls**: hides every action except **Unlock controls**. Skip and next
  episode overlays also stay hidden while locked.

The center row contains fixed controls for:

- rewind 10 seconds;
- play or pause;
- forward 10 seconds.

The bottom area contains the timeline and available actions:

- Tap the timeline to seek to that position. The current timeline accepts taps
  only.
- **Audio** opens only the audio tracks returned by the accepted server source.
  Choosing one sends an exact track override through the server pipeline and
  resumes from the current position.
- **Subtitles** opens only subtitle tracks and an **Off** entry. Choosing one
  uses the same server-owned override flow.
- **Episodes** appears for series episodes. It lists the current episode and
  playable episodes after it, with episode images and labels.
- **Next Episode** appears when a following episode exists and starts it
  immediately.

A **Skip intro**, **Skip recap**, or **Skip credits** action appears while its
Jellyfin media segment is active. During an outro, or when playback ends, the
next episode panel starts a 10 second countdown. **Keep watching** cancels the
automatic advance. **Play next** advances immediately.

## Simulator controls

Use the Simulator keyboard shortcuts while the Simulator window is focused:

- `Command+Left Arrow`: rotate left.
- `Command+Right Arrow`: rotate right.
- `Shift+Command+H`: send the Home gesture.
- `Command+S`: save a Simulator screenshot.

`simctl` handles app lifecycle and captures without using the mouse:

```sh
xcrun simctl list devices booted
xcrun simctl launch booted dev.dahl-jar.homeflix-native
xcrun simctl io booted screenshot .local/player.png
xcrun simctl io booted recordVideo .local/player.mp4
xcrun simctl terminate booted dev.dahl-jar.homeflix-native
```

Stop `recordVideo` with `Control+C`. `simctl` provides lifecycle and capture
commands. Repeatable touch automation belongs in XCUITest or Maestro and should
use accessibility labels such as `Close player`, `Fill screen`, `Pause`, and
`Forward 10 seconds`.

## Validation pass

1. Start an uncached episode and confirm the poster remains visible while real
   pipeline stages progress.
2. Confirm the first failed source is shown before the next source begins.
3. Wait for video and audio, then tap the surface and verify that one control
   layer appears.
4. Test rewind, pause, resume, forward, and a timeline seek.
5. Select a different audio track and subtitle track, then confirm playback
   resumes near the same position.
6. Toggle **Fill screen** and **Fit video** in portrait and landscape.
7. Lock the controls, verify only unlock remains, then unlock them.
8. Open **Episodes** and confirm the current and following episodes have images.
9. Test skip and next episode actions when their media segments become active.
10. Close the player from landscape and confirm the destination screen is
    portrait.
11. Confirm Jellyfin shows one playback session and that pausing produces no
    second audio stream.
