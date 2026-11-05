# GitHub and RuneLite Plugin Hub

Source repository: [ZiolaBleu/pvpplugin](https://github.com/ZiolaBleu/pvpplugin).
The plugin files were uploaded and verified on the repository's `main` branch.
This is development software; it is not yet listed in the Plugin Hub.
Complete `TESTING.md`, including two-client testing and a shared HTTPS deployment,
before requesting public review. No public service is bundled.

For future updates, clone the source repository and run the documented checks
before committing and pushing. The initial source upload uses this repository:

```sh
git clone https://github.com/ZiolaBleu/pvpplugin.git
cd pvpplugin
git rev-parse HEAD
```

Do not add credentials, databases, session backups or the toolchain to Git.
The project ignores common build and secret files. Watch the included GitHub
Actions build. Document actual manual test results in the submission.

The [official submission guide](https://github.com/runelite/plugin-hub#submitting-a-plugin)
requires a fork of `runelite/plugin-hub` and a new file `plugins/pvp-tournament`
on a submission branch. The connected GitHub tools do not expose forking;
create your fork at https://github.com/runelite/plugin-hub/fork.
After manual validation, the manifest should contain exactly:

```properties
repository=https://github.com/ZiolaBleu/pvpplugin.git
commit=FULL_40_CHARACTER_TESTED_COMMIT_SHA
```

Replace the commit placeholder with the full tested commit SHA. Open a pull request against
`runelite/plugin-hub:master`; suggested description:

The package includes `submission/plugins/pvp-tournament` pinned to the uploaded
feature commit and `submission/PR_BODY.md` with a review description. Copy the
manifest into your Plugin Hub fork after completing manual validation; if any
code changes, update the pinned commit and rerun checks first.

> Adds PvP Tournament: private room codes, opt-in registration, single-elimination
> brackets, byes and organizer-confirmed match progression. Requires a trusted
> self-hosted HTTPS service; localhost is for development. No combat automation
> or automatic kill claims. Names are self-declared; credentials are manually
> backed up. Service and privacy documentation are in the source repository.
>
> Validation: Java build, 2 Java tests and 5 service tests passed. Insert actual
> manual client and shared HTTPS test results before requesting review.
>
> Generated-by: OpenAI Codex

Respond to review requests and update the manifest commit after fixes. A PR is
a request for review; maintainers control acceptance, timing and listing. Do
not advertise availability until the PR is merged and the Hub distributes it.
