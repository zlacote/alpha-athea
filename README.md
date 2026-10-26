# Ayana

A terminal for the phone.

A real shell. Each command keeps its answer.

---

## The Model

A session is a stream of bytes. Ayana reads it as **exchanges**: a command, and
the answer it received.

The shell marks where each command begins and ends (OSC 133). What lies between
is the answer, closed by its exit code. Without the marks, the output simply
flows.

Exchanges follow one another, as in a dialog. Each can be folded, searched,
copied or kept.

Programs that draw on the whole screen — `vim`, `top`, `less` — get a screen of
their own. The transcript returns when they exit.

## How it runs

Each session is an append-only journal. On start it is replayed through the
current parser: the same events always give the same transcript, and a parser
fix reaches every past session.

The core is plain Kotlin and knows nothing of Android. The app adds the PTY and
the screens.

## Why Ayana

A terminal assumes a keyboard and a wide screen. A phone has neither: no Esc,
no Ctrl, no arrows, and little width.

Ayana starts from the phone. A key row returns the missing keys, and any key in
it can be moved or replaced. A composer takes the place of the command line.
History is shared by all sessions and never loses what is being written. A long
command can signal when it ends.

## Build

```bash
gradle :core:test :app:testDebugUnitTest :app:assembleDebug
```

JDK 21, Android SDK 37, NDK 28, Gradle 9.

## Status

Before 1.0. The current goal is the first public release.
