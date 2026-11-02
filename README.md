# Independent App Patches

An independent collection of Android app patches. This is a third-party project and is not affiliated with the Morphe project.

## ❓ About

Patch bundles are intended for compatible Android patching tools. Add app-specific patches under `patches/src/main` and extensions under `extensions/` as needed.

### How to use these patches

After publishing this repository on GitHub, replace `OWNER/REPOSITORY` below with its actual address. Then use the button or add the source URL in Morphe Manager.

<a href="https://morphe.software/add-source?github=OWNER/REPOSITORY">
  <img src=".github/assets/add-to-morphe.svg" alt="Add to Morphe" height="48">
</a>

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->

<!-- Do not modify this section by hand. The patch list is generated when the Semaphore pipeline creates a new release.
     
     If you wish for the patches list to be collapsed, then remove the word 'EXPANDED' from the comment tag above.

     If you wish to manually keep this list updated then remove the PATCHES_START and PATCHES_END 
     comment blocks entirely. -->

#### A list of your patches will automatically be shown here after your first patches release is created.

&nbsp;

## 🚀 Getting development started

To start developing and publishing this project:

1. [Setup](https://github.com/MorpheApp/morphe-documentation/blob/main/docs/morphe-development/README.md) your development environment including adding a GitHub PAT as described [here](https://github.com/MorpheApp/morphe-patcher/blob/main/docs/2_1_setup.md#-prepare-the-environment).
2. Create a GitHub repository with a distinct project name from this template, including all branches. Do not use Morphe as the project name or branding; see [NOTICE](NOTICE). Add it as `origin` (`git remote add origin https://github.com/OWNER/REPOSITORY.git`); push your setup changes on `dev`, then merge `dev` into `main` when ready for the first stable release.
3. Connect the repository to Semaphore CI and add a project secret named `morphe-patches-ci` containing `GITHUB_TOKEN` and, if signing releases, `MORPHE_SIGNING_KEY`. The GitHub token must be able to create releases, push release commits/backmerges, and open pull requests.
4. Update the project group and metadata in [patches/build.gradle.kts](patches/build.gradle.kts), including the source URL, author, contact, and website.
5. Replace `OWNER/REPOSITORY` in this README and the issue templates under `.github/ISSUE_TEMPLATE/`.
6. (Optional): Add `patches-bundle.png` to the project if you want a custom icon to show in
   Morphe Manager instead of your GitHub profile avatar.
7. (Optional): Sign your releases, so Morphe Manager refuses a release that was changed or swapped after you published it:
   1. Run `./gradlew generateSigningKey`. It creates `morphe-signing.pub` and `morphe-signing.key`.
   2. Commit `morphe-signing.pub`. Never commit `morphe-signing.key`, it is already ignored by git.
   3. Add the content of `morphe-signing.key` as the `MORPHE_SIGNING_KEY` variable in the
      `morphe-patches-ci` Semaphore project secret, then keep a backup of it somewhere safe and delete the file.

   Every release is signed from then on. If you lose the key or it leaks, create a new one the same way;
   users then have to confirm the new key in Morphe Manager before they get your next release.

🎉 You are now ready to start creating patches!

## 🧑‍💻 Dev usage

To develop and release these patches:

- **Make all changes to the `dev` branch.**
- For local development work build your patches using the gradle task `./gradlew buildAndroid` to generate the mpp file found in `patches/build/libs/patches-*.mpp`. Apply your patches locally using Morphe Desktop tool like any other patch bundle.
- Always use [Semantic commit](https://kapeli.com/cheat_sheets/Semantic_Commits.docset/Contents/Resources/Documents/index) messages for commits. To keep it simple use only 3 commit message types: 
  - `feat: Added a new feature`
  - `fix: Some problem now fixed`
  - `chore: Random change you do not want in the user facing changelog`
- Commits of `fix:` and `feat:` will automatically generate new pre-releases and `chore:` will not create a new release.
- Users can apply your dev branch releases by enabling `pre-release` in Morphe Manager patch sources.
- When your dev branch is ready, and you want a stable release, merge dev branch to main (do not squash, and only merge).
- **Always use semantic release (the Semaphore pipeline)**. Do not manually upload or create releases by hand
  because many files must be updated and the pipeline handles everything.

## 🤓 Tips
- See the [patcher documentation](https://github.com/MorpheApp/morphe-patcher/blob/main/docs/1_patcher_intro.md) for more examples of creating patches and fingerprints.
- Do not use AI to create new release scripts. The Semaphore pipeline here already handles everything.
  If you need something custom with your releases, modify `.semaphore/semaphore.yml` and `.releaserc`
  instead of writing everything new from scratch.
- Do not manually edit or manually commit any generated files such as: `patches-list.json`,
  `patches-bundle.json`, `CHANGELOG.md`. These files are automatically updated by the Semaphore pipeline.
- Do not force push any semantic release commits as that will break all future releases.
  If you need to fix a broken release, it's always easiest to create a new release instead of 
  fixing an existing release.


<!-- The patches end tag is intentionally placed here so the first release will clean up 
     this readme of all developer instructions above. -->
<!-- PATCHES_END -->

### 🛠️ Building locally

- Run `./gradlew buildAndroid`
- The built patches .mpp file is found in `patches/build/libs/patches-*.mpp`
- Patch the mpp file using [Morphe-Desktop](https://github.com/MorpheApp/morphe-desktop)
  like any other patch bundle.

See the [Morphe documentation](https://github.com/MorpheApp/morphe-documentation) for more information.

## 📜 License

Independent App Patches are licensed under the [GNU General Public License v3.0](LICENSE), with additional conditions described in [NOTICE](NOTICE). Preserve required attribution and use distinct branding for derivative works.
