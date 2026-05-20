<h1 align="center">jfrdoc</h1>

<p align="center"><strong>One command. One <code>.jfr</code> file. A real performance report.</strong></p>

<p align="center">
Hand jfrdoc a JVM Flight Recorder recording and it tells you where your<br>
Spring Boot or Quarkus app is burning CPU — with framework-aware<br>
categorization, top callers, and concrete fixes you can ship today.
</p>

<p align="center">
  <img alt="Status"  src="https://img.shields.io/badge/status-pre--alpha-orange">
  <img alt="Java"    src="https://img.shields.io/badge/Java-25%2B-blue">
  <img alt="Agent"   src="https://img.shields.io/badge/agent-zsmith-7e57c2">
  <img alt="Model"   src="https://img.shields.io/badge/model-claude--opus--4--7-cc785c">
  <img alt="License" src="https://img.shields.io/badge/license-MIT-green">
</p>

> 🚧 **Day 5.** CPU-hotspot analysis end-to-end against real Spring Boot recordings. GC, allocation, lock-contention, and I/O tools next.

---

## Why?

`jfr print` and JMC give you raw events. Every team then writes the same rollup shell pipelines, eyeballs the same flame graphs, and writes the same Confluence page. jfrdoc skips the boilerplate: a single Claude-driven agent invokes purpose-built JFR tools and emits a structured markdown report — **findings, evidence, recommendations** — the same shape every time.

A real snippet from [`samples/petclinic-report.md`](samples/petclinic-report.md):

```markdown
## CPU Profile
The category split is highly unusual for a steady-state Spring Boot app:
user_code is only 3.2%, while JDK code dominates at 63.2% and framework
(Spring) at 33.6%.

### Top Hotspots
1. java.util.HashMap.getNode:579        — 346 samples (7.1%, jdk)
2. java.lang.StringLatin1.hashCode:195  — 221 samples (4.5%, jdk)
3. java.util.Arrays.binarySearch0:1713  — 207 samples (4.2%, jdk)
...

## Findings
- 🔴 Spring Boot nested-jar classpath resolution dominates CPU.
  Evidence: Handler.indexOfSeparator 4.0%, URL.<init> 7.0%,
  URLClassPath.getLoader 2.6% — >20% of CPU in nested-jar URL handling.
  Why it matters: every request triggers repeated resource lookups
  through the loader, wasting CPU that should go to business logic
  on a 1-CPU container.
```

---

## Quickstart

**Prerequisites:** [Java 25+](https://jdk.java.net/) (jfrdoc uses JEP 458 source-file mode and implicit classes — no build tool required).

```bash
git clone https://github.com/roz-labs/jfrdoc.git
cd jfrdoc
chmod +x jfrdoc

# Configure your Anthropic API key
mkdir -p ~/.zsmith && cat > ~/.zsmith/app.properties <<'EOF'
anthropic.api.key=sk-ant-...
anthropic.version=2023-06-01
EOF

# Analyze a JFR recording
./jfrdoc analyze recording.jfr \
    --framework spring \
    --container-memory 2Gi \
    --container-cpu 1 \
    > report.md
```

> ⚠️ **Redirect only stdout** (`> report.md`). zsmith's progress chatter goes to stderr — merging the streams with `2>&1` duplicates the report inside the file.

Each analysis costs roughly **$0.50–$2.00** in Anthropic API charges with `claude-opus-4-7`.

No Maven, no Gradle, no npm. `lib/zsmith.jar` (the zero-dependency agent framework jfrdoc is built on) is committed in this repo, so the launcher runs out of the box. See [`lib/README.md`](lib/README.md) for version and rebuild instructions.

---

## What you get today

| | What | Where |
|-|-|-|
| ✅ | **Recording context** — duration, JVM, OS, framework, container limits | `## Recording Context` |
| ✅ | **CPU profile split** — user code vs framework vs JDK vs native | `## CPU Profile` |
| ✅ | **Top hotspots** with top callers and Spring/Quarkus-aware attribution | `### Top Hotspots` |
| ✅ | **Findings** — severity-tagged observations with numeric evidence | `## Findings` |
| ✅ | **Recommendations** — actionable fixes tied to each finding | `## Recommendations` |
| 🛠 | GC pressure, allocation hotspots, lock contention, I/O wait | _roadmap_ |
| 🛠 | OOMKill root cause, container fit, virtual-thread sizing, K8s context | _roadmap_ |

The agent's report is explicit about today's CPU-only scope in its **Analysis Limitations** section, so nothing is hidden.

---

## How it works

jfrdoc reads `.jfr` files via `jdk.jfr.consumer.RecordingFile` from the JDK — **no java agent, no runtime overhead in your app**. A single Claude-driven agent built on [zsmith](https://github.com/AdamBien/zsmith) calls two tools:

| Tool | Purpose |
|-|-|
| `jfr_summary` | High-level metadata, JVM info, event distribution |
| `jfr_top_methods` | CPU hotspot aggregation with framework-aware categorization |

…then synthesizes a markdown report with findings, evidence, and recommendations. As more tools land (GC, allocation, locks, I/O), the same agent reaches for them.

---

## Sample report

[`samples/petclinic-report.md`](samples/petclinic-report.md) — end-to-end run against a 60-second JFR of [Spring PetClinic](https://github.com/spring-projects/spring-petclinic) 4.0.0-SNAPSHOT under HTTP load.
Raw tool outputs (handy as prompt-tuning fixtures): [`petclinic-summary.json`](samples/petclinic-summary.json), [`petclinic-top-methods.json`](samples/petclinic-top-methods.json).

---

## Development

Generate a tiny `.jfr` for fast local iteration (8 s of allocation churn):

```bash
./samples/gen-sample.sh        # writes samples/sample.jfr
```

Exercise each tool directly without the agent loop — useful for verifying the JSON the agent sees:

```bash
./jfrdoc debug-tool jfr-summary     samples/sample.jfr
./jfrdoc debug-tool jfr-top-methods samples/sample.jfr --top-n 10
./jfrdoc debug-tool jfr-top-methods samples/sample.jfr --top-n 5 --framework quarkus
```

### Refresh the dogfood snapshot

`samples/gen-petclinic-sample.sh` clones Spring PetClinic, builds it, runs it under a 60 s JFR window with HTTP load against `/owners`, `/vets`, `/oups`, …, and refreshes the three committed sample artifacts via `./jfrdoc analyze` and the two `debug-tool` commands.

```bash
./samples/gen-petclinic-sample.sh
```

| Env var | When to set it |
|-|-|
| `BUILD_JAVA_HOME` | The PetClinic Maven build fails TLS on your JDK 25. Point this at a JDK whose truststore works (the jfrdoc run itself stays on the Java 25 from `PATH`). |
| `JFRDOC_JAVA_OPTS` | JDK 25 can't reach `api.anthropic.com` behind a TLS-intercepting proxy. e.g. `-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts -Djavax.net.ssl.trustStorePassword=changeit` |
| `PETCLINIC_PORT` | PetClinic's default 8080 conflicts with something local. |
| `JFR_DELAY` / `JFR_DURATION` / `LOAD_SECONDS` | Adjust the recording window or load duration. |

---

## Roadmap

- ✅ **Done** — CLI scaffold, `jfr_summary` + `jfr_top_methods` tools, agent wiring, first dogfood on Spring PetClinic
- 🛠 **Next** — GC / allocation / lock-contention / I/O tools, framework-aware allocation attribution
- 📦 **Next month** — K8s-aware diagnostics, multi-`.jfr` correlation
- ☁ **Future** — hosted SaaS with history and trends

---

## License

MIT — see [LICENSE](LICENSE).
