# Spring AI Bench

## Motivation 

After spending hundreds of hours working with popular AI tools for software development, I’ve been extremely impressed by their capabilities. This is a transformative technology that continues to improve at a rapid pace.  

But one question has always gnawed at me: **how do we quantify this experience?**

In presentations and product demos, you often hear confident claims about new AI stacks or workflows. What’s usually missing is a **quantifiable way** to verify whether those claims hold up.  

Looking deeper, most academic work has tried to answer this with benchmarks. The most prominent in software engineering are the [SWE-bench benchmarks](https://www.swebench.com/original.html). They are widely cited in research papers and marketing material from AI startups, with leaderboards maintained [here](https://www.swebench.com/).  

The more I studied these benchmarks, the more I realized their approach is questionable — and I’m not alone ([Runloop blog](https://www.runloop.ai/blog/swe-bench-deep-dive-unmasking-the-limitations-of-a-popular-benchmark)). Fundamentally, SWE-bench and similar datasets operate in a way that is **not analogous to real software development**. They overwhelmingly use Python, while Java — the dominant enterprise language — is barely represented.  

As someone who wants to understand how AI tools will truly help solve engineering problems, I also believe benchmarks should be **runnable on your own codebases** to evaluate practical effectiveness. Current benchmarks fall short on this dimension.  

That’s why I created **Spring AI Bench**:  
an **open benchmarking suite for Java-centric AI developer agents**.  

It fills a critical gap: today’s benchmarks are Python-biased and built on outdated agent loops that misrepresent developer work. Spring AI Bench instead measures what matters for **enterprise Java development** — issue triage, PR review, integration testing, test coverage, dependency upgrades, compliance, and more.

---

## The Case for Spring AI Bench

- Benchmarks such as “classic” SWE-bench rely on **outdated agent loops** (edit → apply patch → run tests). This gave the illusion of “agency,” but in reality it optimized trial-and-error patching, not developer workflows.  

- Results that look decent in **Python** collapse when tested in **Java**:  
  - **SWE-PolyBench (AWS, 2025):** Across agents, **Python ~20–24%** vs **Java ~11–16%** (TypeScript often just **5–13%**).  
    [arXiv:2501.xxxxx](https://arxiv.org/abs/2501.xxxxx)  
  - **SWE-bench-Java (2024):** Early public runs with the SWE-agent scaffold resolved only **6–10% of verified Java issues** (e.g. GPT-4o 6.6%, DeepSeek-V2 9.9% on 91 verified tasks).  
    Meanwhile, the Python-only SWE-bench Verified benchmark has steadily improved, reaching **74.5% with Anthropic’s Opus 4.1 (Aug 2025)** ([Anthropic blog](https://www.anthropic.com/news/opus-4-1)). In contrast, Java’s early runs remain in the single digits — an **order-of-magnitude gap**.  
  - **SWE-bench-Live (2025):** On new, contamination-resistant issues, even the best agent+model combos top out around **17–19%**, versus >60% on the static Verified split — strong evidence of overfitting.  
    [arXiv:2505.23419](https://arxiv.org/abs/2505.23419)  

**In short:** Verified Python benchmarks reach ~75%, while Verified Java benchmarks remain in the **single-digit to low-teens**. That’s an **order-of-magnitude gap**.

- And if language gaps weren’t enough, the **mini-SWE-agent** adds another kind of evidence: it shows the *mechanics* of these benchmarks are misaligned.  
  - Just ~100 lines of bash-only Python, no tool-calling, linear history.  
  - Yet it achieves **~65–68% on SWE-bench Verified**, on par with the full SWE-agent.  
  - As the authors admit: *“Back then, we placed a lot of emphasis on tools and special interfaces … a lot of this is not needed at all to build a useful agent.”*  
    ([mini-SWE-agent GitHub](https://github.com/SWE-agent/mini-swe-agent))  
  - This tells us SWE-bench Verified **rewards loop mechanics**, not developer behaviors like context-rich planning, PR review, coverage enforcement, or compliance.  

**Bottom line:** In practice, agentic CLIs (Claude Code, Gemini CLI) achieve far higher success rates than the ≈20% reported for Java benchmarks, reinforcing that current evaluations understate real-world capability.

Enterprise Java teams deserve a benchmark that reflects **real software development tasks and workflows** — and one that can be applied directly to **your own Java projects and codebases**. That’s the goal of Spring AI Bench.

---

## Why Another Benchmark?

### The Classic SWE-Agent Loop Is Too Simplistic

The original **SWE-agent** introduced a three-step cycle:  
1. **Edit** the code.  
2. **Apply patch & run tests.**  
3. **Repeat until tests pass (or budget runs out).**

This looked reasonable at first glance, but it was really just trial-and-error patching.  
It gave the illusion of “agency” without planning, context, or review.  
It also ignored essential developer practices like writing tests, integration checks, and coverage or compliance gates.

### Mini-SWE-agent Simplified the Loop Even Further

The later **mini-SWE-agent** reduced the SWE-agent scaffold to ~100 lines of Python:  
- Bash-only execution (no LM tool-calling)  
- Linear history  
- Stateless `subprocess.run` actions  

Even with this minimalist design, it reached **~65–68% on SWE-bench Verified** — essentially the same as the full SWE-agent.  
As the authors note:  
> “Back then, we placed a lot of emphasis on tools and special interfaces … a lot of this is not needed at all to build a useful agent.”  
([mini-SWE-agent GitHub](https://github.com/SWE-agent/mini-swe-agent))  

This result underscores that **SWE-bench Verified primarily rewards loop mechanics**, not whether an agent can act like a real developer.


---

### Why SWE-bench’s Grading Logic Misses the Developer Reality

In SWE-bench (and SWE-bench Verified), agents are judged against **hidden developer-written tests**:  
- The agent never sees the tests.  
- It must produce a patch blind, and then the patch is run against tests written by humans.  
- Passing those tests is the sole success criterion.

> “The tests are not shown to the agent. A proposed edit is evaluated by running both the FAIL_TO_PASS and PASS_TO_PASS tests.”  
([OpenAI, SWE-bench Verified](https://openai.com/index/introducing-swe-bench-verified/))

That grading logic is deeply unrealistic. Even a skilled human would struggle to fix a bug correctly without seeing the tests or writing their own.  
It emphasizes **patch mechanics** rather than reasoning or test-driven development.

Recent analysis confirms the weakness:  
- 7.8% of “passing” patches fail developer tests.  
- 29.6% diverge from the true fix.  
- Reported success rates are inflated by ~6 percentage points.  
([Are Solved Issues in SWE-bench Really Solved Correctly?](https://arxiv.org/abs/2503.15223))

---

### Why Context and Tool Use Matter

Modern agent CLIs — Claude Code, Gemini CLI, and others — succeed or fail based on how well they can **build up and use context** from the codebase, test suite, and build system.  

In practice, this often means calling tools: running tests, generating coverage reports, performing static analysis, or navigating dependency graphs. Standards like **Model Context Protocol (MCP) tools** make this richer context and tool use possible.  

Yet none of today’s benchmarks evaluate this. They don’t measure how well an agent gathers context across files, or how effectively it uses tools to guide reasoning. Instead, they focus narrowly on whether a patch passes hidden tests.

This misses the essence of enterprise software development, where success depends on **building context over time** and **using the right tools to solve the problem**.


---

## Benchmark Landscape

Beyond SWE-bench and its variants, the broader benchmark landscape shows similar limitations.  
Most were designed years ago, focus narrowly on **bug-fixing**, and evaluate against hidden or fixed test suites. This emphasizes mechanical correctness over developer workflows.  

A few key patterns emerge:  
- **Loop mechanics dominate.** SWE-bench and Mini-SWE-agent show that iterating patches until tests pass can achieve competitive scores without real reasoning.  
- **Language bias is severe.** Python benchmarks report the highest success rates, while Java and other enterprise languages lag far behind.  
- **Bug-only focus.** Datasets like Defects4J, Bugs.jar, and BEARS are valuable historically, but don’t measure coverage, integration, or review — the realities of enterprise development.  
- **Narrow domains.** Vul4J captures security cases, but is too specialized to stand alone as a general benchmark.  

Taken together, these benchmarks illustrate the need for **Spring AI Bench**: a suite that emphasizes context, breadth of tasks, and reproducible evaluation aligned with how enterprise developers actually work.

| Benchmark                  | Language Focus | Task Type        | Scope                  | Reported Scores                          | Core Limitation |
|----------------------------|----------------|------------------|------------------------|------------------------------------------|-----------------|
| **SWE-bench (2023)**       | Python-only    | Bug fix loop     | Issue → patch → hidden tests | 1.96% (Claude 2), 33–49% (Verified), up to ~65% with scaffolds | Rewards patch loops; tests hidden from agent. |
| **SWE-bench Lite**         | Python-only    | Bug fix subset   | Small subset of issues | Often <10% solved early on               | Fast to run, but still brittle; loop mechanics dominate. |
| **Mini-SWE-agent (2025)**  | Python-only    | Bash loop agent  | SWE-bench Verified     | ~65–68% (Claude Sonnet 4)                | Just 100 lines; shows loop mechanics dominate. |
| **SWE-bench-Java (2024)**  | Java           | Bug fix loop     | Adapted SWE-bench      | **Java verified ≈6–10%** vs **Python best ≈74.5%** | Clear credibility gap: order-of-magnitude worse in Java. |
| **Anthropic Opus 4.1 (2025)** | Python      | Bug fix Verified | SWE-bench Verified     | **74.5%** (best reported)                 | Python-only; still loop-based. |
| **SWE-PolyBench (2025)**   | Multi (Py/Java/JS/TS) | Bug fix + localization | 2,110 issues, 4 langs | **Python ~20–24%** vs **Java ~11–16%**, TS ~5–13% | Reveals language bias; non-Python much harder. |
| **Defects4J (2014→)**      | Java           | Bug fix dataset  | 835 bugs              | Execution-based                          | Bug-only; ignores coverage, review, integration. |
| **Bugs.jar (2018)**        | Java           | Bug fix dataset  | 1,158 bugs            | Execution-based                          | Larger scale, but bug-only. |
| **BEARS (2019)**           | Java           | Bug fix CI-mined | CI failing→passing    | Execution-based                          | Adds CI realism, but still patch-focused. |
| **Vul4J (2022)**           | Java           | Security fix     | 79 vulnerabilities    | Proof-of-Vuln tests                      | Security-narrow; not general. |

---

## Tracks

Spring AI Bench defines tracks that map directly to **real enterprise developer workflows**:

- ✅ **Test Coverage Uplift**  
- ✅ **Issue Analysis & Labeling**  
- ✅ **Pull Request Review**  
- ✅ **Integration Testing**  
- ✅ **Bug Fixing**  
- ✅ **Dependency Upgrades**  

---

## Why Broader Than SWE-bench?

Looking across the benchmark landscape, several consistent limitations appear:

- **Classic SWE-agent loops are too shallow.** They reward edit/patch/test cycles, not planning or context.  
- **Mini-SWE-agent highlights the same pattern.** A 100-line bash loop matched the original SWE-agent (~65% Verified), showing that benchmark scores are driven by loop mechanics more than developer reasoning.  
- **SWE-bench’s grading logic is unrealistic.** Agents must match hidden human tests they never saw. Even a skilled human would struggle under that setup, and “passing” doesn’t guarantee correctness.  
- **Performance collapses outside Python.** SWE-PolyBench and SWE-bench-Java both expose the bias: results are 2–3× worse for Java.  
- **Reported correctness is shaky.** Studies show up to 30% of “passing” patches diverge from the true fixes, inflating scores.

These issues make it clear: today’s benchmarks don’t evaluate the kinds of capabilities enterprise teams actually care about. They measure mechanical patch loops under artificial test harnesses.

---

Spring AI Bench asks a bigger question:  

**Can AI act as a true Java developer agent?**  
- Not just fixing bugs,  
- But analyzing and labeling issues,  
- Reviewing pull requests,  
- Running integration tests,  
- Raising coverage,  
- Cleaning up static analysis issues,  
- Migrating APIs,  
- Upgrading dependencies,  
- Keeping builds compliant.  

That’s the standard enterprise developers hold themselves to — and the standard we should evaluate AI against.

---



## Additional Findings

Spring AI Bench builds not only on gaps in existing benchmarks but also on insights from applied research and experiments:

- **AI as the reviewer, not just the actor.**  
  Most benchmarks rely on a single binary metric (e.g., “did the patch pass the test suite?”). In practice, AI systems themselves can serve as effective reviewers.  
  Combining a **quantitative measure** (such as code coverage) with an **AI-based qualitative review** (similar to a PR review) produces richer evaluations.  
  This makes it possible to grade not only correctness, but also clarity, maintainability, and alignment with project style.  
  Spring AI Bench will uniquely incorporate this “AI reviewing AI” methodology.

- **Issue labeling beyond academic baselines.**  
  *Colavito et al., MSR 2024* evaluated GPT-3.5 on 400 GitHub issues with **4 simple labels** (*bug, feature, documentation, question*), achieving ~0.81–0.83 F1.  
  Using the same train/test split methodology, a dataset of **Spring AI GitHub issues** with **100+ technical, domain-specific labels** (e.g., vector stores, LLM clients, Spring modules) was classified.  
  Despite the far greater complexity, the results reached **82.1% F1**, with perfect scores (F1 = 1.0) on 20+ labels and excellent results (≥0.9 F1) on several more.  
  This suggests that AI can match or exceed academic baselines even in much harder, real-world, multi-label classification tasks.

- **Integration testing with log analysis.**  
  Maintaining complex integration examples is an ongoing challenge. A prototype system was developed that uses AI to **analyze client/server logs**, with prompts generated directly from the project’s README, to determine whether the system still functions correctly after changes.  
  This allowed AI to reason about logs in a human-like way and validate end-to-end behavior.  
  The work-in-progress ([spring-ai-examples/integration-testing](https://github.com/spring-projects/spring-ai-examples/tree/main/integration-testing)) shows how AI can assist with integration validation — one of the most critical but time-consuming enterprise tasks.

- **PR review automation at scale.**  
  A PR review automation system has been built for Spring AI repositories, turning what was once a manual, multi-step process into an **AI-driven workflow with structured reports**.  
  - **Scale:** 20+ batch runs, processing more than 150 PRs.  
  - **Performance:** Current runs typically handle 10–17 PRs per batch in ~4–5 minutes per PR. Early iterations encountered failures, but improvements in conflict handling, compilation repair, and prompt strategies have made the system stable and reliable in practice.  
  - **Outputs:** Each PR produces a Markdown review report, test logs, and an [HTML dashboard](docs/images/PR-Dashboard.pdf).  
  - **Analysis quality:** Reports include risk assessment, architecture evaluation, backport analysis, and prioritized recommendations. See example assessments: [Assessment 1](docs/images/assesment-1.pdf), [Assessment 2](docs/images/assesment-2.pdf).  

These results suggest that AI can meaningfully contribute to **PR review at enterprise scale**, providing consistent analysis and freeing maintainers to focus on decision-making.

---

Taken together, these findings incidate that Spring AI Bench can evaluate far more than whether a patch passes hidden tests. They point to the need for benchmarks that capture how agents:  
- review and grade work,  
- label and triage issues,  
- validate integrations, and  
- support PR workflows at scale.  

These directions will inform the design of Spring AI Bench tracks, ensuring evaluation is grounded in the actual workflows enterprise developers rely on.


## Next steps

As a starting point, Spring AI Bench will launch with a **Coverage Uplift track**:  

- **Setup:** Small/medium Java projects with missing tests.  
- **Task:** Agents are asked to add tests until modules meet JaCoCo thresholds.  
- **Scoring:**  
  - Coverage delta (before vs after).  
  - Build must remain green.  

This provides:  
- **Clear, objective metrics.**  
- **Fast iteration.**  
- **Direct enterprise relevance** (test coverage gates are common in CI/CD).  

Then the previous work on issue labelling, PR assessment, and integration testing will be brought into the repository.


---

## References

- **SWE-bench** – *Can Language Models Resolve Real-World GitHub Issues?* (Princeton, 2023)  
  [arXiv](https://arxiv.org/abs/2310.06770) | [Project site](https://www.swebench.com/original.html)

- **SWE-PolyBench** – *A Multi-Language Benchmark for Repository-Level Evaluation of Coding Agents* (AWS AI Labs, 2025)  
  [arXiv](https://arxiv.org/abs/2504.08703) | [GitHub](https://github.com/amazon-science/SWE-PolyBench)

- **SWE-bench-Java** – *Extending SWE-bench to the Java Ecosystem* (2024)  
  [arXiv](https://arxiv.org/abs/2408.14354)

- **Mini-SWE-Agent** – Minimalist 100-line agent achieving ~65% on SWE-bench Verified (2025)  
  [GitHub](https://github.com/SWE-agent/mini-swe-agent)

- **Anthropic Opus 4.1** – Reported 74.5 % Verified on SWE-bench (Anthropic, 2025)  
  [Anthropic blog](https://www.anthropic.com/news/opus-4-1)

- **SWE-bench-Live** – *Evaluating Agents on Live, Contamination-Resistant Issues* (2025)  
  [arXiv](https://arxiv.org/abs/2505.23419)

- **Defects4J** – *A Database of Real Faults for Controlled Testing Studies in Java* (ISSTA 2014)  
  [PDF](https://homes.cs.washington.edu/~rjust/publ/defects4j_issta_2014.pdf) | [GitHub](https://github.com/rjust/defects4j)

- **Bugs.jar** – *A Large-Scale, Diverse Dataset of Real-World Java Bugs* (MSR 2018)  
  [PDF](https://cs.gmu.edu/~winglam/publications/2018/bugs-dot-jar.pdf) | [MSR Data Showcase](https://2018.msrconf.org/details/msr-2018-Data-Showcase-Papers/12/Bugs-jar-A-Large-scale-Diverse-Dataset-of-Real-world-Java-Bugs)

- **BEARS** – *Mining and Reproducing Bugs and Fixes from Continuous Integration* (2019)  
  [GitHub](https://github.com/bears-bugs/bears-benchmark)

- **Vul4J** – *A Dataset of Real-World Java Vulnerabilities with Proof-of-Vulnerability Tests* (MSR 2022)  
  [GitHub](https://github.com/Samsung/vul4j)

- **Migration-Bench** – *A Benchmark for Java Library and Version Migration* (2025)  
  [arXiv](https://arxiv.org/abs/2501.01532) | [GitHub](https://github.com/KTH/MigrationBench)

- **RefactoringMiner** – *Automated Refactoring Detection* (TSE 2020)  
  [GitHub](https://github.com/tsantalis/RefactoringMiner)
