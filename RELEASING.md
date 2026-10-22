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

If the workflow fails before the "Sign and publish" step, nothing was uploaded: fix
the problem, delete the release and the tag, and start again. If it fails during or
after the upload, check the *Deployments* page on central.sonatype.com before
retrying: Maven Central never overwrites a published version.

To bump the default version used by local builds and snapshots, change the
`revision` property in `pom.xml` after a release.

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

### 2. Maven Central account that is not a person

The namespace `org.congocc` on <https://central.sonatype.com> is currently
verified for a personal account. To make it project-owned:

1. The namespace owner contacts
   [Central Support](https://central.sonatype.org/pages/support/) and asks for
   *Organization Management* access for the `org.congocc` namespace.
2. In **Publish → Organizations**, invite the other maintainers (they need their own
   central.sonatype.com login first) and grant them the `org.congocc` namespace. Make
   at least two people **Admin** so the project does not depend on one account.
3. Generate the user token that CI will use: log in, **View Account → Generate User
   Token**. Any member with publish rights on the namespace can generate it. Prefer a
   dedicated login (e.g. a `congocc-releases` account created with a shared mailbox)
   that is a member of the organization, so a maintainer leaving does not revoke CI.

The user token consists of a *username* and a *password*; both are random strings,
not the login credentials.

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
