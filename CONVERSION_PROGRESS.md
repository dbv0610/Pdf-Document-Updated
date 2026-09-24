# android_office Java → Kotlin Conversion Progress

Last updated: 2026-09-23 (session on branch `conver-modue`)

## Summary

| Metric | Count |
|---|---|
| Java files remaining | **1,878** |
| Kotlin files converted | **211** |
| Total (original module) | ~2,089 |

Build state: **green** (2026-09-23, clean build after merging round-1 lanes). Incremental builds can falsely report "package ... does not exist" for Kotlin classes → add `:android_office:clean`.

## How to compile (GitHub Packages token is expired — 401)

The plugin `com.azura.plugin.android-application:1.0.0` can't resolve online. Rebuild the local-maven workaround each session:

1. Copy `~/.gradle/caches/modules-2/files-2.1/com.azura.plugin*` jars/poms/modules into a maven-layout dir (group dots → slashes) under the session scratchpad.
2. Init script must use `settingsEvaluated { pluginManagement.repositories.maven(...); dependencyResolutionManagement.repositories.maven(...) }` (NOT `allprojects` — repo mode is FAIL_ON_PROJECT_REPOS).
3. Compile:
   ```
   sh gradlew :android_office:compileDebugJavaWithJavac --console=plain --init-script <scratchpad>/local-repo.init.gradle.kts
   ```

## Completed packages (green build)

- ✅ `constant` (14), `objectpool` (4), `res` (2), `utils` (2), `pdf` (6), `macro` (14)
- ✅ `wp` (42 files). 16 broken mechanical conversions were hand-rewritten from `git show HEAD:<path>.java`:
  LeafView, BNView, EncloseCharacterView, ObjView, LineView, PageView, ShapeView, TableView,
  TableLayoutKit, ViewFactory, WPLayouter, WPSTRoot, WPViewKit, LayoutKit, PositionLayoutKit
  (+ ParagraphView made `open`, PrintWord 1-line null fix)
- ✅ `java/awt` part 1 (19 files, earlier session): Dimension, Image, Shape, Stroke + geom iterators (Arc/Cubic/Ellipse/Line/Quad/Rect/RoundRect), ChainEnd, CurveLink, Dimension2D, Edge, Order0, Order1, exceptions
- ✅ `java/awt` part 2 (this session, 9 files): PathIterator, Point2D, RectangularShape, Rectanglef, FlatteningPathIterator, Ellipse2D, RoundRectangle2D, Order2, **Order3 (⚠️ not compile-verified yet)**

## ⚠️ Known quality caveat — mechanically converted wp files

The `wp` package mechanical conversion **stubbed or dropped method bodies while still compiling**.
Confirmed gutted & rewritten: `LayoutKit.kt`, `PositionLayoutKit.kt`.
Spot-checked OK: PageRoot, NormalRoot, model files, BreakPagesCell, FEElement.
**Not body-verified**: Word.kt, WPControl, WPEventManage, PrintWord, WPFind, CellView, RowView, TitleView, WPDocument.
If wp rendering bugs appear, diff these against `git show HEAD:<path>.java` first.
Detection: compare `grep -c "fun "` vs original method count; grep `") {}$"` for empty bodies.

## Pending conversion (1,975 files)

### Next up: `java/` — 15 files remaining (~24k lines, OpenJDK ports)

Recommended order (dependencies: Kotlin can't see Java package-private members, so
Path2D must be converted before/with GeneralPath; Curve before its remaining users):

| File | Lines | Notes |
|---|---|---|
| java/awt/geom/Crossings.java | 524 | uses Curve |
| java/awt/geom/AreaOp.java | 561 | uses Curve, Edge |
| java/awt/geom/Area.java | 728 | uses AreaOp, Crossings |
| java/awt/geom/Rectangle2D.java | 913 | base for Rectangle |
| java/awt/geom/Line2D.java | 1,113 | |
| java/awt/Color.java | 1,135 | mostly standalone |
| java/awt/Rectangle.java | 1,199 | extends Rectangle2D |
| java/awt/geom/Curve.java | 1,207 | base of Order0–3 (already Kotlin) |
| java/awt/geom/QuadCurve2D.java | 1,385 | statics used by FlatteningPathIterator |
| java/awt/geom/Arc2D.java | 1,555 | extends RectangularShape (Kotlin) |
| java/awt/geom/CubicCurve2D.java | 1,740 | statics used by FlatteningPathIterator |
| java/awt/geom/Path2D.java | 2,647 | convert together with GeneralPath |
| java/awt/geom/GeneralPath.java | 111 | extends Path2D.Float (pkg-private fields) |
| java/awt/geom/AffineTransform.java | 4,239 | |
| java/util/Arrays.java | 4,396 | standalone JDK port |

### Then, package by package (compile after each)

| Package | Files | Notes |
|---|---|---|
| simpletext | 33 | view/model base classes used by wp — signatures must keep Java-compatible (IView, AbstractView, ViewKit, AttrManage…) |
| pg | 27 | PowerPoint viewer |
| system | 39 | IControl, MainControl, SysKit… |
| officereader | 43 | app layer (AppActivity etc.) |
| common | 80 | shapes, picture, bg, borders — heavily referenced by wp Kotlin |
| ss | 68 | spreadsheet viewer |
| thirdpart | 230 | achartengine, emf, mozilla |
| fc | 1,440 | split by sub-package: hssf 546, hslf 166, dom4j 148, hwpf 146, ss 108, poifs 69, util 39, ddf 37, openxml4j 36, hpsf 36, codec 23, sl 21, ppt 19, xls 14, fs 10, doc 6, pdf 4, usermodel 3, 9 root files |

## Conversion conventions (keep consistent)

- Work from `git show HEAD:<path>.java`; delete the `.java` in the same step as writing `.kt`.
- Constants → `object` + `const val`; Short/Byte chains as `(PREV + 1).toShort()`.
- Byte/Short comparisons & arithmetic: `.toInt()` on both sides; `when` over Byte constants is fine Byte-to-Byte.
- Java compound narrowing `x -= floatArr[i]` (int x) → `x = (x - floatArr[i]).toInt()`.
- Property + `fun getX()`/`fun isX()` with same JVM name → **Platform declaration clash**; fix with `@JvmField` on the field or rename the private backing field.
- Base classes need `open` / `open fun` when subclassed (ParagraphView, LeafView pattern).
- Statics Java callers use → `companion object` + `@JvmStatic`; interface constants → companion `const val`.
- Java statics are NOT inherited into Kotlin subclass scope — qualify (`Curve.round(...)`).
- `Math.round(double)` returns Long in Kotlin; `Math.ceil` needs `.toDouble()`.
- Control chars: use \\u0007 \\u000B \\u000C \\u0002 escapes (no \\f in Kotlin).
- Preserve original behavior including bugs, `Log.e` calls, and Chinese comments.

## 2026-09-23 — parallel lanes (Codex + Claude Code)

Each lane is a private copy of the repo under `/Volumes/Data/Android/AS005-lanes/<lane>/`; shared brief `AS005-lanes/LANE_BRIEF.md`,
shared init script `AS005-lanes/local-repo.init.gradle.kts` (local maven with the azura plugin). Each lane writes `LANE_PROGRESS.md`.
Coordinator copies lanes' converted files back into this tree, then compiles clean and fixes cross-lane breaks.

| Lane | Agent | Scope | Status |
|---|---|---|---|
| codex-officereader | Codex CLI | officereader → pg | running |
| claude-awt | Claude Code subagent | java/awt+util (15) → simpletext | running |
| claude-ss | Claude Code subagent | ss (68) | running |

Next lanes after merge: system, common, thirdpart, then fc sub-packages.

## Planned phase 2 — coroutine refactor (after system/common/ss/pg are Kotlin)

Lanes convert threading faithfully and mark `// TODO(coroutine): ...`. Then one unified refactor:
- Add `org.jetbrains.kotlinx:kotlinx-coroutines-android` to `android_office`.
- One `CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)` owned by `MainControl` (cancelled in `dispose()`); injectable `DispatcherProvider` (main/io/default) for tests.
- `FileReaderThread` / `BackReaderThread` → `scope.launch(io)` + `withContext(main)` for UI callbacks.
- `LayoutThread` (wp), PG/SS background layout → `Dispatchers.Default`, cancellable via `Job`, `ensureActive()` in loops.
- `PictureConverterThread`, `VectorgraphConverterThread`, `PictureManage` → `Dispatchers.IO` with a limited-parallelism dispatcher (`IO.limitedParallelism(2)`).
- `SafeAsyncTask`, `officereader/search/Search`, `PDFFind` → suspend functions / `Flow` for progressive results.
- `Handler(...).post` UI hops → `withContext(Dispatchers.Main)`.

## 2026-09-23 — round 1 merged, round 2 launched

Round 1 merged into main (green, 211 .kt): java/awt+util complete (15), ss 49/68, pg 22/27 + officereader 11/43 (Codex).
Merge gotcha: only copy a lane's modified *pre-existing* file from the lane that actually changed it (CurveLink.kt got clobbered by a stale copy once).
Codex round 1 code is valid but dense (multiple statements per line) — reformat pass wanted later.
⚠️ Codex round 1 installed an Android Studio plugin `~/Library/Application Support/Google/AndroidStudio2026.1.1/plugins/lane-j2k`
(auto-J2K for pg/officereader on project open) and restarted AS. User moved it to ~/Desktop/lane-j2k on 2026-09-23 (disabled).
Brief now forbids IDE automation; Codex now runs with `-s workspace-write --add-dir ~/.gradle`.

Round 2 lanes (all re-synced from merged main):
| Lane | Agent | Scope | Log |
|---|---|---|---|
| codex-officereader | Codex | pg (5) → officereader (32) | AS005-lanes/codex-officereader-run1.log |
| codex-system | Codex | system (39) | AS005-lanes/codex-system-run1.log |
| claude-ss | Claude subagent | ss remaining (19) | lane LANE_PROGRESS.md |
| claude-awt | Claude subagent | simpletext (33) | lane LANE_PROGRESS.md |

Merge procedure: for each lane, copy .kt files that differ from main, delete .java that the lane deleted, then
`:android_office:clean :android_office:compileDebugJavaWithJavac` with init script `AS005-lanes/local-repo.init.gradle.kts`.
Next after round 2: common (80), thirdpart (230), fc sub-packages.

## 2026-09-23 — PRIORITY: document readers first (user request)

Word/Excel/PowerPoint reading matters most. Rendering: wp (done), ss, pg. Readers: fc/doc (6), fc/xls (14), fc/ppt (19);
their POI libs: hwpf 146 (Word), hssf 546 + fc/ss 108 (Excel), hslf 166 (PPT); shared: openxml4j 36, dom4j 148, poifs 69, util 39, ddf 37.
officereader (app UI) and thirdpart are deprioritized (thirdpart lane had converted ~18 files; officereader partial — both left as-is).
Machine has 24 GB RAM → max ~5 concurrent lanes (each gradle daemon -Xmx4g).

Round 2 lanes rescoped:
| Lane dir | Agent | Scope | Log |
|---|---|---|---|
| codex-officereader | Codex | finish pg → fc/ppt → fc/hslf | codex-officereader-run3.log |
| codex-thirdpart | Codex | fc/doc → fc/xls → fc/hwpf | codex-thirdpart-run3.log |
| codex-system | Codex | system | codex-system-run2.log |
| claude-ss | Claude | ss remaining | lane LANE_PROGRESS.md |
| claude-awt | Claude | simpletext | lane LANE_PROGRESS.md |
Codex sandbox needs `-c sandbox_workspace_write.network_access=true` (gradle uses sockets).
Next when Claude lanes free up: shared fc deps (openxml4j, dom4j, poifs, util, ddf), then fc/hssf + fc/ss (Excel), then common.

## 2026-09-23 — simpletext merged (main green: 244 .kt / 1,845 .java)
- simpletext (33) done by Claude lane; it also added `!!`/nullability fixes in wp/*, pdf/PDFFind, pg/PGFind.
- Merging now uses `AS005-lanes/merge-lane.sh <lane> [base]` — 3-way (`git merge-file`) against a base snapshot:
  round-2 lanes (codex-officereader, codex-thirdpart, codex-system, claude-ss) → base `base-round2-src` (default);
  claude-awt lane (re-synced after simpletext merge) → base `base-claude-fc-src`. Script prints CONFLICT lines to resolve by hand.
- claude-awt lane new scope: fc/util → fc/poifs → fc/openxml4j → fc/ddf → fc/dom4j.
- Codex `exec` tends to stop after 1–2 batches → use `AS005-lanes/codex-loop.sh <lane> <prompt-file> [max]`: relaunches until `<lane>/LANE_DONE`
  exists; logs `AS005-lanes/<lane>-loop-N.log`. Running for codex-officereader (prompt-ppt.txt) and codex-thirdpart (prompt-docxls.txt).
- Codex avoids huge files. Reassigned to Claude (claude-ss lane, after its ss work merges): fc/doc/DOCReader (2,386 lines),
  fc/doc/DOCXReader (5,759), pg/control/Presentation (1,329), pg/view/SlideDrawKit (836). Codex prompts updated to skip them.

## 2026-09-23 — all lanes handed to Codex (Claude usage limit)
Claude subagents stopped mid-batch; Codex loops now run every lane (`AS005-lanes/codex-loop.sh`, logs `<lane>-loop-N.log`):
| Lane dir | Prompt | Scope | Merge base |
|---|---|---|---|
| claude-ss | prompt-big.txt | ss rest → DOCReader, DOCXReader, Presentation, SlideDrawKit (chunked) | base-round2-src |
| claude-awt | prompt-fcshared.txt | fc util → poifs → openxml4j → ddf → dom4j | base-claude-fc-src |
| codex-officereader | prompt-ppt.txt | fc/ppt → fc/hslf (pg leftovers skipped) | base-round2-src |
| codex-thirdpart | prompt-docxls.txt | fc/xls → fc/hwpf (+ fc/doc small files done) | base-round2-src |
| codex-system | prompt-system2.txt | system (loop starts after current run) | base-round2-src |
Check: `ls AS005-lanes/*/LANE_DONE`, `pgrep -fl codex-loop`. Merge each finished lane: `./merge-lane.sh <lane> [base]`, then clean compile main.
Merge order suggestion: codex-system first (most shared), then claude-ss, codex-officereader, codex-thirdpart, claude-awt; resolve CONFLICT lines by hand.

## 2026-09-23 — STOP CONDITION (user): stop once the office *reading* part is converted
Final scope: fc/doc, fc/xls, fc/ppt, ss, pg (+ system, fc/util in progress). POI libs (hwpf/hslf/hssf/fc.ss/dom4j/poifs/openxml4j/ddf),
common, thirdpart, officereader stay Java for now. Prompts updated with "FINAL SCOPE" → each lane creates LANE_DONE and its loop exits.
`AS005-lanes/finish-watch.sh` waits for all loops to exit, merges every lane into main (merge-lane.sh) and clean-compiles;
result in `AS005-lanes/merge-report.txt`. Then: resolve CONFLICT lines / compile errors, update counts here, and STOP (no new lanes).
- Big files moved back to Claude: new lane `AS005-lanes/claude-big` (synced from main == base `base-claude-fc-src`), Claude subagent converting
  SlideDrawKit → Presentation → DOCReader → DOCXReader; writes LANE_DONE when finished. claude-ss Codex lane now only finishes ss.
- finish-watch.sh does NOT merge claude-big — merge it manually: `./merge-lane.sh claude-big base-claude-fc-src`.
- Hourly session cron (job 7ecc8e98, :17) auto-resumes claude-big after usage-limit resets and does the final merge. Session-only:
  if Claude Code was closed, just start a new session and say "làm tiếp theo CONVERSION_PROGRESS.md".

## 2026-09-24 — all Codex lanes merged, claude-big partial merged (main green: 426 .kt / 1,670 .java)
- merge-report.txt errors were from a bad claude-awt merge (relative base path → `cd` failed); already cleaned up; fresh clean build green.
  ⚠️ Always pass an ABSOLUTE base dir to merge-lane.sh.
- claude-big merged (`merge-lane.sh claude-big /Volumes/Data/Android/AS005-lanes/base-claude-fc-src`): SlideDrawKit.kt, Presentation.kt.
  Fixed nullability at call sites (`!!`) in PGControl, PGPrintMode, SlideShowView, Presentation.
- Remaining in final scope: fc/doc/DOCReader.java (2,386), fc/doc/DOCXReader.java (5,759) — converting directly in main (no lane).
- Session cron 9a5ab141 (:17 hourly) resumes this after usage-limit resets.
- ✅ fc/doc/DOCReader.kt done (hand-converted, green). Also fixed wp/model RowElement/TableElement.getElementForIndex → `IElement?`
  (lane had `!!`, which NPE'd at end-of-row in DOC table reading) + TableLayoutKit `!!`.
- ⚠️ 2026-09-24 05:55 android_office/build.gradle.kts was edited outside Claude (compileSdk=37, but SDK only has android-37.0/37.1 → "Failed to find Platform SDK android-37").
  Not touched by Claude. For verification Claude uses scratch init script overriding compileSdk=36 (android_office only).
- 2026-09-24: STOPPED by user (cost). DOCXReader.java stays Java (partial Kotlin parts only in a session scratchpad, discarded). Cron cancelled.
  Build check: app ConvertViewModel `presentation.pgModel` → `getPGModel()!!` (Presentation is Kotlin now).
  Remaining blocker: android_office minSdk=26 (uncommitted change) vs app minSdk=24 → manifest merger fails. With minSdk aligned, `:app:assembleDebug` is green.
- 2026-09-24: Kotlin bumped 2.2→2.4 → `kotlinOptions { jvmTarget }` is an error; migrated both modules to `kotlin { compilerOptions { jvmTarget.set(JVM_17) } }`.
  User's DOCXReader.kt had only part 1/6 (Java deleted) → handed to Codex: `AS005-lanes/codex-docx.sh` (prompt-docx.txt, refs in docx-ref/,
  logs codex-docx-N.log, done marker AS005-lanes/docx-DONE). After done: verify method count vs docx-ref/DOCXReader.java.orig, clean compile.
- 2026-09-24 06:4x: user stopped Codex DOCXReader run (all 9 chunks written, compile not yet green).
- 2026-09-24 ~06:57: Codex finished: DOCXReader.kt compiles, WPFind.kt = user's new search version (+focusByCurrent/findAll/focusBy kept). Clean build :android_office + :app:compileDevDebugKotlin green (with min24 override).
