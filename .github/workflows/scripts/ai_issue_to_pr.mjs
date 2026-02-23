import fs from "fs";
import { execSync } from "child_process";
import OpenAI from "openai";

const {
  OPENAI_API_KEY,
  OPENAI_MODEL,
  ISSUE_TITLE,
  ISSUE_BODY,
  ISSUE_NUMBER,
  REPO,
  GH_TOKEN
} = process.env;

if (!OPENAI_API_KEY) {
  console.error("Missing OPENAI_API_KEY secret.");
  process.exit(1);
}

const model = OPENAI_MODEL || "gpt-4.1-mini";
const openai = new OpenAI({ apiKey: OPENAI_API_KEY });

function sh(cmd) {
  return execSync(cmd, { stdio: "pipe" }).toString("utf8").trim();
}

function safeRepoSnapshot(maxFiles = 300) {
  return sh(`bash -lc "git ls-files | head -n ${maxFiles}"`);
}

function readFileSafe(path, maxBytes = 250000) {
  try {
    const buf = fs.readFileSync(path);
    return buf.length > maxBytes ? buf.slice(0, maxBytes).toString("utf8") : buf.toString("utf8");
  } catch {
    return null;
  }
}

const fileList = safeRepoSnapshot();

let indexPath = null;
const candidates = ["index.html", "public/index.html", "views/index.html", "src/index.html"];
for (const c of candidates) {
  if (fs.existsSync(c)) { indexPath = c; break; }
}
const indexContent = indexPath ? readFileSafe(indexPath) : null;

const system = `
You are a senior software engineer working inside a Git repository.
Return ONLY a git-apply compatible unified diff patch.
Do not include explanations.
Make minimal safe edits.
Keep existing features unless asked otherwise.
`;

const user = `
ISSUE TITLE:
${ISSUE_TITLE}

ISSUE BODY:
${ISSUE_BODY}

REPO FILE LIST (partial):
${fileList}

INDEX FILE PATH:
${indexPath || "(not found)"}

INDEX FILE CONTENT (if found):
${indexContent || "(not found)"}

Return ONLY a unified diff patch.
`;

const resp = await openai.chat.completions.create({
  model,
  messages: [
    { role: "system", content: system },
    { role: "user", content: user }
  ],
  temperature: 0.2
});

const patch = resp.choices?.[0]?.message?.content?.trim() || "";
if (!patch.startsWith("diff --git")) {
  console.error("Model did not return a valid unified diff patch.");
  console.error(patch.slice(0, 5000));
  process.exit(1);
}

const branch = `ai/issue-${ISSUE_NUMBER}`;

sh(`bash -lc "git checkout -b ${branch}"`);

fs.writeFileSync("ai.patch", patch, "utf8");
try {
  sh(`bash -lc "git apply --whitespace=fix ai.patch"`);
} catch {
  console.error("Failed to apply patch. Patch was:");
  console.error(patch.slice(0, 8000));
  process.exit(1);
}

sh(`bash -lc "git add -A"`);

try {
  sh(`bash -lc "git commit -m \\"AI: Issue #${ISSUE_NUMBER} - ${String(ISSUE_TITLE).replace(/"/g,'')}\\""`);
} catch {
  console.log("Nothing to commit.");
  process.exit(0);
}

sh(`bash -lc "git push -u origin ${branch}"`);

process.env.GITHUB_TOKEN = GH_TOKEN;

try {
  sh(`bash -lc "gh pr create --repo ${REPO} --head ${branch} --base main --title \\"AI: ${String(ISSUE_TITLE).replace(/"/g,'')}\\" --body \\"Closes #${ISSUE_NUMBER}\\""`);
} catch {
  sh(`bash -lc "gh pr create --repo ${REPO} --head ${branch} --base master --title \\"AI: ${String(ISSUE_TITLE).replace(/"/g,'')}\\" --body \\"Closes #${ISSUE_NUMBER}\\""`);
  }
