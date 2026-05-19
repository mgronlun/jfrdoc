# jfrdoc

**AI-powered JFR analyzer that tells you why your Spring Boot / Quarkus pod is OOMKilled — even when heap looks fine.**

> Status: 🚧 Pre-alpha. Day 1 scaffold.

## Install

Prerequisites:

- **Java 25** or newer (uses implicit classes, text blocks, source-file mode)
- **zsmith.jar** — the zero-dependency agent framework jfrdoc is built on

Build zsmith from source and drop the jar into `lib/`:

```bash
git clone https://github.com/AdamBien/zsmith
cd zsmith && ./zb.sh
cp out/zsmith.jar /path/to/jfrdoc/lib/zsmith.jar
```

Then make sure `jfrdoc` is executable:

```bash
chmod +x jfrdoc
```

No Maven, no Gradle, no npm. Java 25's source-file mode runs the script directly.

## Usage

```bash
./jfrdoc analyze samples/test.jfr \
    --framework quarkus \
    --container-memory 2Gi \
    --container-cpu 1
```

Produces a markdown report on stdout (zsmith progress chatter goes to stderr,
so `./jfrdoc analyze ... > report.md` gives you a clean file).

Requires `anthropic.api.key` configured in `~/.zsmith/app.properties`:

```
anthropic.api.key=sk-ant-...
anthropic.version=2023-06-01
```

Each analysis costs roughly $0.50–$2.00 in API charges with zsmith's
default model (currently `claude-opus-4-7`).

## Development

Generate a sample `.jfr` recording for local testing:

```bash
./samples/gen-sample.sh
```

Each individual tool can be exercised directly via the `debug-tool`
subcommand — useful for verifying the JSON it returns to the agent
loop without going through the full analysis pipeline:

```bash
./jfrdoc debug-tool jfr-summary samples/test.jfr
./jfrdoc debug-tool jfr-top-methods samples/test.jfr --top-n 10
./jfrdoc debug-tool jfr-top-methods samples/test.jfr --top-n 5 --framework quarkus
```

## What it diagnoses

- OOMKill root cause (heap + off-heap + native memory vs container limit)
- GC pressure under container memory limits
- Allocation hotspots in your code
- CPU throttling effects on GC pauses and latency
- Thread model issues (platform vs virtual threads, pool sizing vs CPU limits)

## How it works

jfrdoc reads `.jfr` recordings via `jdk.jfr.consumer.RecordingFile` from the JDK. It runs a multi-agent system built on [zsmith](https://github.com/AdamBien/zsmith), with specialized tools per concern (CPU, GC, allocation, threads) that each query a slice of the recording. The agents then produce a markdown report with findings, supporting evidence, and concrete recommendations.

## Roadmap

- **This week:** JFR tool prototypes + first end-to-end analysis
- **Next month:** K8s-aware diagnostics, multiple `.jfr` support
- **Future:** hosted SaaS with history and trends

## License

MIT — see [LICENSE](LICENSE).
