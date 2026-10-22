# Releasing ccc-mvn

Releases are made from the GitHub UI. Nothing runs on a maintainer's machine and no
personal credentials are involved: the workflow in `.github/workflows/deploy.yml`
builds, signs and publishes to Maven Central with credentials that belong to the
project.

## Making a release

1. Make sure `main` is green (the *Build and Test* workflow, including the
   *Release build (no publish)* job).
2. Go to **Releases → Draft a new release**.
3. **Choose a tag**: enter the new version, e.g. `v2.2.0`, and let GitHub create it
   from `main`. The version is taken from the tag; `pom.xml` does not need to be
   edited. `v2.2.0` and `2.2.0` both work, `v2.2.0-RC1` is allowed, `-SNAPSHOT` is not.
4. Click **Generate release notes** (categories come from `.github/release.yml`).
5. Click **Publish release**. Drafts and pre-releases that are not published do
   not trigger anything.
6. Watch the *Release to Maven Central* workflow. If the `maven-central`
   environment has required reviewers, one of them has to approve the run first.
7. When the workflow is green the artifacts are on
   <https://central.sonatype.com/namespace/org.congocc> (visible on
   <https://repo1.maven.org/maven2/org/congocc/> after a few minutes) and the jars
   are attached to the GitHub release.

If the workflow fails before anything was uploaded (any step before "Sign and
publish", or that step failing before "Uploading"), fix the problem on `main` and
re-run it for the same tag: **Actions → Release to Maven Central → Run workflow**,
enter the tag (e.g. `v2.2.0`). The workflow file is taken from `main`, the code from
the tag, so the release does not need to be recreated. If it fails during or after
the upload, check the *Deployments* page on central.sonatype.com before retrying:
Maven Central never overwrites a published version.

The `revision` property in `pom.xml` is the *next* development version with a
`-SNAPSHOT` suffix (e.g. `2.2.0-SNAPSHOT` while 2.2.0 is unreleased). It only
affects local builds; the release workflow overrides it with the tag version. After
a release, bump it to the next `-SNAPSHOT` so local builds never collide with a
published version.

## Snapshots

`.github/workflows/snapshot.yml` publishes the `revision` from `pom.xml` to
<https://central.sonatype.com/repository/maven-snapshots/> on every push to `main`
(and on *Run workflow*), as long as that version ends in `-SNAPSHOT`. It uses the
same secrets as the release workflow. Snapshots are not validated by Sonatype, are
overwritten on each push and deleted after 90 days.

One-time setup: on central.sonatype.com open *View Namespaces*, use the dropdown
on `org.congocc` and select **Enable SNAPSHOTs**.

## One-time setup (project credentials)

Everything below lives at the **congo-cc organization level** where possible, so
it is not tied to any single maintainer's GitHub or Sonatype account.

### 1. Signing key

Maven Central requires every artifact to be signed with a key whose public part is
on a key server. Use one **project key**, not a personal key.

A project key already exists: `CongoCC <CongoCC@CongoCC.CongoCC>`, fingerprint
`1D7F 8AB9 A859 18BB 16F5 C18A E54C 7472 F8E0 98ED`, published on
keyserver.ubuntu.com and keys.openpgp.org. Export it from the machine that holds it:

```sh
gpg --armor --export-secret-keys E54C7472F8E098ED > congocc-signing-key.asc
```

If a new key is ever needed:

```sh
gpg --batch --quick-generate-key "CongoCC Release Signing <releases@congocc.org>" rsa4096 sign never
gpg --list-keys --keyid-format long          # note the key id
gpg --keyserver keyserver.ubuntu.com --send-keys <KEYID>
gpg --keyserver keys.openpgp.org --send-keys <KEYID>
gpg --armor --export-secret-keys <KEYID> > congocc-signing-key.asc
```

Store the exported private key somewhere the maintainers share (password manager
of the org), then delete the `.asc` file.

### 2. Maven Central user token

The namespace `org.congocc` on <https://central.sonatype.com> already has several
publisher users (currently `stbischof` and two accounts of Jonathan Revusky; see
*View Namespaces → org.congocc → users*). Any of them can generate the user token
that CI uses: log in, **View Account → Generate User Token**. The token consists of
a *username* and a *password*, both random strings, not the login credentials.

Because more than one person can publish, a maintainer leaving does not block
releases: another publisher generates a new token and replaces the two secrets.

Keep at least two active publisher users on the namespace. To add one, the new
person creates a central.sonatype.com login and an existing publisher asks
[Central Support](https://central.sonatype.org/pages/support/) to add them, or
uses *Organization Management* (Publish → Organizations) if that is enabled for the
namespace.

Note: Maven Central enforces monthly publishing limits per account (shown on the
Portal); a token from a dedicated account would share that account's quota.

### 3. GitHub secrets

Create these as **organization secrets** (congo-cc → Settings → Secrets and
variables → Actions, repository access: `ccc-mvn`), or as repository secrets if
org-level is not possible:

| Secret                   | Value                                                       |
| ------------------------ | ----------------------------------------------------------- |
| `MAVEN_CENTRAL_USERNAME` | user-token *username* from central.sonatype.com             |
| `MAVEN_CENTRAL_TOKEN`    | user-token *password* from central.sonatype.com             |
| `GPG_PRIVATE_KEY`        | full content of `congocc-signing-key.asc` (ASCII-armored)   |
| `GPG_PASSPHRASE`         | passphrase of the signing key                               |

The old repository secrets `OSSRH_USERNAME`, `OSSRH_TOKEN` and
`OSSRH_GPG_SECRET_KEY_PASSWORD` are no longer used and can be deleted.

### 4. GitHub environment (optional but recommended)

Repository → Settings → Environments → `maven-central` (the workflow creates it on
first run if it does not exist):

* **Required reviewers**: the maintainers who may approve a publish. Then anyone
  with write access can *create* a release, but it only reaches Maven Central after
  one of the reviewers approves the workflow run.
* **Deployment branches and tags**: restrict to tags matching `v*` if you want.

Environment secrets can be used instead of org secrets if the secrets should be
visible to this workflow only.

### 5. Verify without publishing

The *Release build (no publish)* job in `mvn_verify.yml` runs on every pull request
and builds the release profile with signing disabled. Locally:

```sh
mvn -Prelease verify -Dgpg.skip=true
```
