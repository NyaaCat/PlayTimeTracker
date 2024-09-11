
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


## Change Log

### 0.9-SNAPSHOT

- [Tech] port to 1.21.1 and CI migration;
- [Tech] publish to github packages
- **[BREAKING] Abandon support for other Bukkit/Spigot, Only support PaperMC ** 

  we don't have enough effort for different distributions

### 0.10-alpha.1

- [Feature] **Rewards storage & any-time acquire**: Now rewards will be record when a mission is completed, and you can use `/ptt acquire` to acquire it at anytime, no need to worry about missing it!

- [Feature/WIP] Reward Module instead of use command all the time. Currently only support ECO Rewards. Command Rewards and Item Rewards are WIP.

- [Feature] Add `/ptt listrewards <missionName|all>` to show rewards awaiting for acquiring.

- [Configuration] `missions.yml` :

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
          sync-ref-cache-time: 12800 # make sure players completing the mission within `sync-ref-cache-time` get the same reward amount (that is, use the same reference vault value). In milliseconds.
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
        reward3:
          __class__: cat.nyaa.playtimetracker.config.data.CommandRewardData
          pre-command: ''  # command executed immediately when the mission is completed; can be empty string
          command: /tell %%player_name%% hello, world! # command executed when the reward is acquired
      notify: true
  ```

- [Configuration] lang

  - `command.listrewards.*`
  - `manual.listrewards.*`

  ```yaml
  command:
    listrewards:
      show_all: 有 %s 个任务奖励待领取
      show: 有 %s 个 %s 任务奖励待领取
      empty_all: 没有可以领取的任务奖励
      empty: 没有可以领取的 %s 任务奖励
  manual:
    listrewards:
      description: 查看所有奖励
      usage: /ptt listrewards <奖励名称|all>
  ```

- [Tech] Add some Unit Tests.

- [Tech] Recover to use NyaaCat CI

### 0.10-alpha.2

- [Feature] Add notification for ECO Rewards. Now when acquire ECO Rewards, it displays how much you get.

- [Feature] `/ptt listrewards all` now display detailed information of how many rewards for each mission.

- [Configuration] lang

  - `command.listrewards.err`

  - `command.listrewards.show_item`

  - `message.reward.notify` for login notification

  - `message.reward.eco` for ECO Rewards acquiring detailed information

  ```yaml
  command:
    listrewards:
      err: 查询失败
      show_item: "- 任务 %s: %s"  # first %s is mission name; second %s is rewards count
  message:
    reward:
      notify:
        command: /ptt acquire
        msg: 您有 %s 个任务奖励可以领取
      eco:
        success: 经济奖励%s%s已经添加到您的账户  # first %s is amount; second %s is eco-unit
  ```

- [Fix] Database wrapper log

- [Fix] remove `NotifyAcquireTask` since it should not work.

- [Fix] pipeline for github-package and NyaaCat CI

- [Tech/WIP] PaperMC Adventure for messages instead of legacy Bukkit API


### 0.10-alpha.3

- [Fix] When use transfer type ECO Reward, if there are multiple players complete the mission at the same time, the reward amount decreases by the order of player list.

- [Feature]  (As the [Fix] Above) Add configuration in `mission.yml` for transfer type ECO reward: 

  `missions[].reward-list[].sync-ref-cache-time` 

  to make sure players completing the mission within `sync-ref-cache-time` get the same reward amount (that is, use the same reference vault value).

  The time unit is milliseconds. Default is 12800 (12.8s = 4 * 64gt). Set to zero or `-1` to disable this feature.

- [Feature] Add Command Rewards. Now you can execute a command when a mission is completed and a reward is acquired.

- [Feature] Add configuration in `mission.yml` for later send information of awaiting rewards when login:
  
  `login-check-delay-ticks`

  to make it possible to show this information at the end of login information. The time unit is ticks. Default is 20 (1s).

- [Tech] When distributing rewards, database will delete invalid records so that it will not occupy player's rewards list forever.

## TODO

#### 0.10-alpha

- [x] add reward database for keeping completed rewards
- [x] add reward module: eco

#### 0.10

- [x] add reward module: command
- [ ] add reward module: item
- [x] logging system
- [ ] i18n: https://docs.papermc.io/paper/dev/component-api/i18n#examples
- [ ] mission completion notification (sound or Advancement)

#### 1.0
- [ ] Use time-wheel instead of 1gt polling
   - [ ] Optimize Expressions
   - [ ] calculate next mission completion time
   - [ ] time-wheel for mission completion
   - [ ] command for querying next mission completion time
- [ ] Make all db operations async
   - [ ] Remove `cat.nyan.playtimetracker.db.connection`
   - [ ] Make `CachedXXXTable` and async methods
- [ ] disable plugin: self-disable
