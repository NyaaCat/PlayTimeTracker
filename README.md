# PlayTimeTracker
Playtime tracking plugin for ~~Bukkit/Spigot~~ PaperMC

## Configuration

### missions.yml

```yaml
missions:
  eco:
    __class__: cat.nyaa.playtimetracker.config.data.MissionData
    group: []
    expression: lastSeen>1&&dailyTime>1&&weeklyTime>1&&monthlyTime>1&&totalTime>1&&1==2
    reset-daily: true
    reset-weekly: false
    reset-monthly: false
    reward-list: # can be multiple rewards; the key is not important, but reward will be executed in key order
      reward1:
        __class__: cat.nyaa.playtimetracker.config.data.EcoRewardData
        type: TRANSFER  # TRANSFER, ADD
        ref-vault: $system # "$system" or player uuid
        ratio: 0.01
        min: 1.0
        max: 1024.0
        vault: $system # "$system" or player uuid  # same as /eco transfer 0.01:1:1024:$system $system %player_name%
    notify: true
  daily:
    __class__: cat.nyaa.playtimetracker.config.data.MissionData
    group: []
    expression: dailyTime>3600000
    reset-daily: true
    reset-weekly: false
    reset-monthly: false
    reward-list:
      reward2:
        __class__: cat.nyaa.playtimetracker.config.data.EcoRewardData
        type: ADD
        amount: 100 # same as /eco add 100 %player%
    notify: true

```


## Build locally for *github packages*

1. Clone the repository

2. [Acquire a Github personal access token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens) with at least read:packages scope.

3. Modify the `gradle.properties` file (OR `~/.gradle/gradle.properties` for global)

   ```
   gpr.user=<GITHUB_USERNAME>
   gpr.key=<GITHUB_TOKEN>
   ```
4. Run the build and publish (locally) with maven.

5. **If you modified `gradle.properties` under this repo, When committing changes, make sure to remove what you add in step 3. so that `GITHUB_TOKEN` is not exposed.**


## TODO

#### 0.10-alpha

- [x] add reward database for keeping completed rewards
- [x] add reward module: eco

#### 0.10

- [ ] add reward module: command
- [ ] add reward module: item
- [ ] logging system

#### 1.0

- [ ] Make all db operations async
   - [ ] Remove `cat.nyan.playtimetracker.db.connection`
   - [ ] Make `CachedXXXTable` and async methods
- [ ] Use time-wheel instead of 1gt polling
- [ ] Optimize Expressions

