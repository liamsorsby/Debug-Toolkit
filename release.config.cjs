module.exports = {
  branches: ["main"],
  tagFormat: "v${version}",
  plugins: [
    [
      "@semantic-release/commit-analyzer",
      {
        preset: "conventionalcommits"
      }
    ],
    [
      "@semantic-release/release-notes-generator",
      {
        preset: "conventionalcommits"
      }
    ],
    [
      "@semantic-release/exec",
      {
        prepareCmd:
          "bundle exec fastlane android build_release " +
          "version_name:${nextRelease.version} version_code:$VERSION_CODE",
        publishCmd:
          "bundle exec fastlane android publish_release " +
          "version_name:${nextRelease.version}"
      }
    ],
    [
      "@semantic-release/github",
      {
        assets: [
          {
            path: "app/build/outputs/apk/release/app-release.apk",
            label: "Debug Toolkit APK"
          },
          {
            path: "app/build/outputs/bundle/release/app-release.aab",
            label: "Debug Toolkit App Bundle"
          }
        ]
      }
    ]
  ]
};
