# GitHub Actions Workflows

## Release Workflow

The release workflow allows maintainers to create a new release and publish it to Maven Central with a single manual trigger.

### How to Use

1. Go to the [Actions tab](../../actions) in GitHub
2. Select the "Release" workflow from the left sidebar
3. Click "Run workflow"
4. Enter the release version (e.g., `3.0.0`)
5. Click "Run workflow"

### What the Workflow Does

The release workflow automates the entire release process:

1. **Validates** the version format (must be X.Y.Z, no SNAPSHOT)
2. **Updates** the version in `build.gradle`
3. **Creates and pushes** a git tag (e.g., `v3.0.0`)
4. **Builds and tests** the project
5. **Signs** the artifacts with GPG
6. **Publishes** to Sonatype/Maven Central
7. **Closes and releases** the Sonatype staging repository
8. **Creates** a GitHub release
9. **Updates** `build.gradle` to the next SNAPSHOT version (e.g., `3.1.0-SNAPSHOT`)

### Required Secrets

The following secrets must be configured in the repository:

- `SONATYPE_USER` - Sonatype username
- `SONATYPE_PASSWORD` - Sonatype password
- `GPG_PRIVATE_KEY` - GPG private key for signing (in ASCII-armored format)
- `GPG_PASSPHRASE` - GPG key passphrase

### Notes

- The workflow automatically increments the minor version for the next SNAPSHOT (e.g., 3.0.0 → 3.1.0-SNAPSHOT)
- The release is published directly to Maven Central without requiring manual Nexus Repository Manager interaction
- All artifacts are signed with GPG as required by Maven Central

## Build Workflow

The build workflow runs automatically on all pushes to master and pull requests. It:

1. Validates the Gradle wrapper
2. Builds and tests the project
3. Publishes test results
4. Runs SonarCloud analysis (if token is available)

The build workflow no longer publishes to Maven Central - use the Release workflow instead.
