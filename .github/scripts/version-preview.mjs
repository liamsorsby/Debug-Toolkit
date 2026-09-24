import { execFileSync } from "node:child_process";
import { analyzeCommits } from "@semantic-release/commit-analyzer";
import semver from "semver";

const title = process.env.PR_TITLE;
if (!title) {
  throw new Error("PR_TITLE is required");
}

const releaseType = await analyzeCommits(
  { preset: "conventionalcommits" },
  {
    commits: [{ message: title }],
    logger: console
  }
);

let latestVersion;
try {
  const latestTag = execFileSync(
    "git",
    ["describe", "--tags", "--abbrev=0", "--match", "v[0-9]*"],
    { encoding: "utf8", stdio: ["ignore", "pipe", "ignore"] }
  ).trim();
  latestVersion = semver.clean(latestTag);
} catch {
  // semantic-release starts an untagged repository at 1.0.0.
}

const nextVersion = releaseType
  ? latestVersion
    ? semver.inc(latestVersion, releaseType)
    : "1.0.0"
  : latestVersion ?? "none";

const output = [
  "## Release preview",
  "",
  `- Current version: \`${latestVersion ?? "none"}\``,
  `- Release type: \`${releaseType ?? "none"}\``,
  `- Proposed version: \`${nextVersion}\``
].join("\n");

console.log(output);
if (process.env.GITHUB_STEP_SUMMARY) {
  const { appendFileSync } = await import("node:fs");
  appendFileSync(process.env.GITHUB_STEP_SUMMARY, `${output}\n`);
}
