# Athea

A terminal that reads like a messenger.

Instead of a character grid, Athea turns a shell session into a conversation:
your commands appear as bubbles on the right, their output as blocks on the
left. It is a real shell underneath — a PTY, a shell, your real `$PATH` — with a
chat-shaped reading layer on top.

The problem it solves is a phone one. A soft keyboard has no Ctrl, Esc, Tab or
arrow keys, and a raw terminal is miserable on a small screen. Athea adds a key
row for exactly those keys, turns scrollback into collapsible messages, and
replaces line editing with a composer.

## How a shell becomes a chat

The trick is shell integration. The app ships an rc file that makes the shell
emit **OSC 133** markers into the output stream, and `StreamParser` recognises
them:

| Marker | Meaning | Athea does |
| --- | --- | --- |
| `OSC 133;A` | prompt about to be drawn | ignored |
| `OSC 133;B` | prompt written | ignored |
| `OSC 133;C` | command started | opens a running output block |
| `OSC 133;D;<code>` | command finished | closes it and records the exit code |

Everything between `C` and `D` becomes the body of that command's answer. A
submission is a `CommandBlock`; the output that follows is an `OutputBlock`. No
markers means no block boundary, which is why behaviour degrades gracefully on
shells that do not load the rc file.

Each session journals its events to disk, and a restart replays the journal
through the current parser — so parser fixes apply retroactively to old
sessions.

## Layers

Athea is two Gradle modules. `:core` is plain Kotlin with no Android or Compose
dependency and carries the unit tests. `:app` is the Android shell around it.

```
core/            pure JVM, tested without a device
  terminal/      TerminalEngine — the only interface a shell backend implements
  parse/         StreamParser — UTF-8 across chunks, SGR → styled spans, OSC 133
  transcript/    TranscriptBuilder — events → blocks, collapse rules, raw projection
  session/       Workspace — sessions, drafts, history, favourites, settings
  core/journal/  SessionJournal — append-only binary log, replayed on start
app/             Android
  engine/        NativeShellEngine (PTY via JNI), SshShellEngine (stub)
  ui/            MainViewModel adapts Workspace to Compose; ui/main renders it
```

The core is deterministic: the same journal events always produce the same
transcript, which is how sessions survive application restarts. Everything
platform-specific (PTY, Compose, Android logging) sits behind an interface or in
`app/`, so a different terminal backend replaces `engine/` without touching the
core or the screens.

## Known limits

- **Full-screen TUI programs are not supported.** `vim`, `top`, `less`, `fzf`
  redraw the screen with cursor positioning and the alternate screen buffer.
  The parser strips every escape sequence except SGR, so these render as
  garbage. The `RAW` display mode is *not* a terminal emulator — it is the same
  parsed stream without bubbles, so it does not help. A real grid renderer for
  intercepted blocks is the missing piece.
- **Interactive programs without a completion marker** (REPLs such as `python`
  or `psql`, `tail -f`) never emit `OSC 133;D`. Their output block stays open,
  which is reported honestly as "running".
- **Submitting while a command runs** does not cancel it. The bytes are
  buffered by the tty and run afterwards; the pending bubble shows immediately
  and the running block keeps growing above it, so the transcript order still
  matches the real stream order.
- **`\r` overwrites are approximated.** Carriage return is treated as "discard
  the line" rather than "overwrite from column 0", so progress bars can lose
  their prefix.

## Build

Requires JDK 21, Android SDK 35, NDK 26.1.10909125. There is no Gradle wrapper
checked in; CI installs Gradle 8.9.

```bash
gradle :core:test :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease
```

`ci/emulator_run.sh` drives the debug APK on an emulator: installs, launches,
types a command, then reads back the session journal and watches for crashes.
