# Ayana

A terminal shaped for the phone.

A real shell underneath. On top, every command and its answer stay together —
to fold, find, copy and come back to.

---

## The model

A shell session is a stream of bytes. Ayana reads it as **commands** and their
**answers**.

The shell is started with a small rc file that marks the stream with **OSC 133**:
where a command starts and where it ends, with its exit code. Everything between
the two marks is that command's answer. Without the marks there are no
boundaries, and the output simply flows.

So the transcript reads like a conversation: your command, then what the shell
said back. That shape is a consequence, not the point — the point is that each
exchange is one piece you can work with.

Programs that take over the whole screen (`vim`, `top`, `less`) get a screen of
their own while they run, and the transcript comes back when they exit.

## How it runs

Each session writes its events to an append-only journal. On start, the journal
is replayed through the current parser, so the same events always give the same
transcript, and a parser fix reaches old sessions too.

The core is plain Kotlin, with no Android in it, and carries the tests. The app
around it holds the PTY, the screens and nothing the core depends on.

```
core/   the stream parser, the transcript, sessions and the journal
app/    the PTY, the screens, settings
```

## On the phone

A soft keyboard has no Esc, Ctrl, Tab or arrows: a key row brings them back,
and every key in it can be moved, renamed or replaced. Commands are written in
a composer rather than on a line. History is shared by all sessions, and paging
through it never loses what you were writing. A long command can wake you when
it ends.

## Build

JDK 21, Android SDK 37, NDK 28. No Gradle wrapper is checked in; CI uses
Gradle 9.

```bash
gradle :core:test :app:testDebugUnitTest :app:assembleDebug
```

`ci/emulator_run.sh` runs the debug build on an emulator: it types a command,
reads the session journal back and watches for crashes.

## Status

Before 1.0, and it still changes often.
