# PlayTimeTracker
Playtime tracking plugin for ~~Bukkit/Spigot~~ PaperMC


## Build locally

1. Clone the repository

2. [Acquire a Github personal access token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens) with at least read:packages scope.

3. Modify the `gradle.properties` file

   ```
   gpr.user=<GITHUB_USERNAME>
   gpr.key=<GITHUB_TOKEN>
   ```
4. Run the build and publish (locally) with maven.

5. **When committing changes, make sure to remove what you add in step 3. so that `GITHUB_TOKEN` is not exposed.**