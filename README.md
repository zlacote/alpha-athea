# Ayana

A terminal for Android, made for the phone screen.

Ayana runs the phone's own shell in a real terminal (a PTY) and shows the session
as a conversation: the command you sent, and under it what the shell answered.
Long answers fold. Any command or answer can be searched, copied, or kept as a
favorite.

## On a phone

- **The keys a phone keyboard lacks.** A row above the keyboard with Esc, Tab,
  Ctrl, Alt, the arrows, Home and End, and the symbols it hides a page away:
  `/`, `|`, `~`, `-`. Any key can be moved, edited or replaced.
- **A composer, not a command line.** Commands are written like messages, with
  a full-screen editor for long ones. Up and Down page through history shared
  by all sessions, without losing what you were writing.
- **Sessions that stay.** Several at once, each kept across restarts.
- **Programs that take the whole screen** (`vim`, `top`, `less`) get a screen of
  their own; the conversation comes back when they exit.
- **Files from the phone**, several at a time, copied or linked into the shell's
  folder.
- **Long commands** can buzz and notify you when they end, also in the
  background.
- **Its own look:** glass surfaces, an accent colour, fonts, a wallpaper.

## How it works

The shell is started with a small rc file that marks each command with OSC 133:
where it starts, and where it ends with its exit code. Everything between the
marks is that command's answer. Without the marks the output simply flows.

Each session is written to a journal as it happens. On start the journal is
replayed through the current parser, so sessions come back as they were, and a
parser fix reaches old sessions too.

`core/` is plain Kotlin with no Android in it: the parser, the transcript and
sessions, with the tests. `app/` is the Android side: the PTY and the screens.

## Build

```bash
gradle :core:test :app:testDebugUnitTest :app:assembleDebug
```

JDK 21, Android SDK 37, NDK 28, Gradle 9. Every green build on `main` publishes
its version as a release, with an APK per processor and a universal one.

## Status

Early, version 0.8. Things still change between versions.
